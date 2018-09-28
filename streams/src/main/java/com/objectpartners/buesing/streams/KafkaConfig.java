package com.objectpartners.buesing.streams;

import com.objectpartners.buesing.avro.Count;
import com.objectpartners.buesing.avro.Distance;
import com.objectpartners.buesing.avro.NearestAirport;
import com.objectpartners.buesing.avro.Record;
import com.objectpartners.buesing.common.util.BucketFactory;
import com.objectpartners.buesing.common.util.DistanceUtil;
import com.objectpartners.buesing.streams.client.Geolocation;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.UsePreviousTimeOnInvalidTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableKafka
@EnableKafkaStreams
@Slf4j
public class KafkaConfig {

    private final KafkaStreamProperties kafkaStreamProperties;

    @Autowired
    private StreamsBuilder streamsBuilder;

    final SpecificAvroSerde<Distance> distaneSerde = new SpecificAvroSerde<>();
    final SpecificAvroSerde<Record> recordSerde = new SpecificAvroSerde<>();
    final SpecificAvroSerde<NearestAirport> nearestAirportSerde = new SpecificAvroSerde<>();
    final SpecificAvroSerde<Count> countSerde = new SpecificAvroSerde<>();

    private static final String HOST = "http://localhost:9080";
    private Geolocation geolocation = Feign.builder()
            .options(new Request.Options(200, 200))
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(Geolocation.class, HOST);

    public KafkaConfig(final KafkaStreamProperties kafkaStreamProperties) {
        this.kafkaStreamProperties = kafkaStreamProperties;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("KafkaConfig configured.");

        final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", kafkaStreamProperties.getSchemaRegistryUrl());
        distaneSerde.configure(serdeConfig, false);
        recordSerde.configure(serdeConfig, false);
        nearestAirportSerde.configure(serdeConfig, false);
        countSerde.configure(serdeConfig, false);
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public StreamsConfig kStreamsConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put("schema.registry.url", kafkaStreamProperties.getSchemaRegistryUrl());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaStreamProperties.getBootstrapServers());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaStreamProperties.getApplicationId());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        //props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, UsePreviousTimeOnInvalidTimestamp.class.getName());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, ExceptionHandler.class);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new StreamsConfig(props);
    }

    @Bean
    public KStream<String, Record> red() {
        return streamsBuilder
                .stream("red", Consumed.with(Serdes.String(), recordSerde));
        // .peek((key, value) -> log.debug("flights : key={}, value={}", key, value));
    }

    @Bean
    public KStream<String, Record> blue() {
        return streamsBuilder
                .stream("blue", Consumed.with(Serdes.String(), recordSerde));
        //.peek((key, value) -> log.debug("enemies : key={}, value={}", key, value));
    }

    @Bean
    public KStream<String, NearestAirport> nearestAirport() {
        return red().map((KeyValueMapper<String, Record, KeyValue<String, NearestAirport>>) (key, value) -> {
            String airport = geolocation.closestAirport(
                    value.getLocation().getLatitude(),
                    value.getLocation().getLongitude()
            ).getCode();
            return KeyValue.pair(
                    key,
                    new NearestAirport(
                            airport,
                            value.getLocation().getLatitude(),
                            value.getLocation().getLongitude()
                    )
            );
        }).through("red.nearest.airport", Produced.with(Serdes.String(), nearestAirportSerde));
    }

    @Bean
    public KStream<String, Count> nearestAirportCount() {

        KStream<String, Count> bean = nearestAirport()
                .selectKey((key, value) -> value.getAirport())
                .groupByKey()
                .windowedBy(TimeWindows.of(WINDOW))
                .aggregate(
                        () -> 0,
                        (key, value, aggregate) -> aggregate + 1
                        , Materialized.with(Serdes.String(), Serdes.Integer()))
                .toStream((wk, v) -> wk.key())
                .mapValues(Count::new)
                .through("red.nearest.airport.count", Produced.with(Serdes.String(), countSerde));

        return bean;
    }

    private static final long WINDOW = 4 * 60 * 60 * 1000L;

    @Bean
    public KStream<String, Distance> distance() {

        final BucketFactory bucketFactory = new BucketFactory(3.0);

        KStream<String, Distance> result =
                red().map((KeyValueMapper<String, Record, KeyValue<String, Record>>) (key, value) -> KeyValue.pair(bucketFactory.create(value.getLocation()).toString(), value))
                .selectKey((key, value) -> key)
                .join(blue().flatMap((key, value) -> bucketFactory
                        .createSurronding(value.getLocation())
                        .stream()
                        .map((b) -> KeyValue.pair(b.toString(), value))
                        .collect(Collectors.toList())).selectKey((key, value) -> key),
                        (value1, value2) -> {

                            double d = DistanceUtil.distance(value1.getLocation().getLatitude(), value1.getLocation().getLongitude(),
                                    value2.getLocation().getLatitude(), value2.getLocation().getLongitude());

                            return new Distance(value1, value2, d);
                        }, JoinWindows.of(WINDOW), Joined.with(Serdes.String(), recordSerde, recordSerde))
                .through("distance", Produced.with(Serdes.String(), distaneSerde));

        return result;
    }

    @Bean
    public KTable<Windowed<String>, Distance> closest() {

        KTable<Windowed<String>, Distance> result = distance()
                .selectKey((key, value) -> value.getRed().getAircraft().getTransponder())
                .groupByKey()
                .windowedBy(TimeWindows.of(WINDOW))
                .aggregate(
                        () -> new Distance(null, null, Double.MAX_VALUE),
                        (key, value, aggregate) -> (value.getDistance() < aggregate.getDistance()) ? value : aggregate);

        result.toStream()
                .map((KeyValueMapper<Windowed<String>, Distance, KeyValue<?, ?>>) (key, value) -> KeyValue.pair(key.key().toString(), value))
                .to("closest");

        System.out.println("@@@");
        System.out.println(streamsBuilder.build().describe());
        System.out.println("@@@");

        return result;
    }

}
