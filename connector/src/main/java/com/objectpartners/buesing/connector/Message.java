package com.objectpartners.buesing.connector;


import lombok.ToString;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.opensky.model.StateVector;

@ToString
public class Message extends Struct {

    public final static Schema SCHEMA = SchemaBuilder.struct().name("com.objectpartners.buesing.avro.Record").version(1)
            .field("origin", Schema.STRING_SCHEMA)
            .field("aircraft", SchemaBuilder.struct().name("com.objectpartners.buesing.avro.Aircraft").version(1)
                    .field("transponder", Schema.STRING_SCHEMA)
                    .field("callsign", Schema.STRING_SCHEMA)
                    .build()
            )
            .field("location", SchemaBuilder.struct().name("com.objectpartners.buesing.avro.Location").version(1)
                    .field("latitude", Schema.FLOAT64_SCHEMA)
                    .field("longitude", Schema.FLOAT64_SCHEMA)
                    .build()
            )
            .field("altitude", Schema.OPTIONAL_FLOAT64_SCHEMA)
            .field("onGround", Schema.BOOLEAN_SCHEMA)
            .field("velocity", Schema.OPTIONAL_FLOAT64_SCHEMA)
            //.field("trueTrack", Schema.FLOAT64_SCHEMA)
            .field("verticalRate", Schema.OPTIONAL_FLOAT64_SCHEMA)
            .build();

    public Message(StateVector message) {
        super(SCHEMA);
        this.put("origin", message.getOriginCountry());
        this.put("location", new Struct(SCHEMA.field("location").schema())
                .put("latitude", message.getLatitude())
                .put("longitude", message.getLongitude())
        );
        this.put("aircraft", new Struct(SCHEMA.field("aircraft").schema())
                .put("transponder", message.getIcao24())
                .put("callsign", message.getCallsign())
        );
        this.put("altitude", message.getGeoAltitude());
        this.put("onGround", message.isOnGround());
        this.put("velocity", message.getVelocity());
        this.put("verticalRate", message.getVerticalRate());
    }

}