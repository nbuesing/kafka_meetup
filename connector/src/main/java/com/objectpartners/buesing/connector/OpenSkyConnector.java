package com.objectpartners.buesing.connector;


import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Generic Kafka Connector which takes a list of channels from an IRC server and creates 1 or more tasks depending
 * on maxTasks configuration argument.
 */

public class OpenSkyConnector extends SourceConnector {

    public static final String TOPIC_CONFIG = "topic";
    public static final String RED_TOPIC_CONFIG = "red.topic";
    public static final String BLUE_TOPIC_CONFIG = "blue.topic";

    private String topic = null;
    private String redTopic = null;
    private String blueTopic = null;

    @Override
    public String version() {
        System.out.println("*** VERSION");
        return AppInfoParser.getVersion();
    }

    @Override
    public void start(Map<String, String> props) {
        topic = props.get(TOPIC_CONFIG);
        redTopic = props.get(RED_TOPIC_CONFIG);
        blueTopic = props.get(BLUE_TOPIC_CONFIG);
    }

    @Override
    public Class<? extends Task> taskClass() {
        System.out.println("*** taskClass");
        return OpenSkyTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        int numTasks = 1;
        for (int t = 0; t < numTasks; t++) {
            Map<String, String> config = new HashMap<>();
            config.put(TOPIC_CONFIG, topic);
            config.put(RED_TOPIC_CONFIG, redTopic);
            config.put(BLUE_TOPIC_CONFIG, blueTopic);
            configs.add(config);
        }
        return configs;
    }

    @Override
    public void stop() {

    }

    @Override
    public ConfigDef config() {

        System.out.println("*** CONFIG");
        return new ConfigDef()
                .define(TOPIC_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "kafka topic to publish the feed into")
                .define(RED_TOPIC_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "kafka topic to publish the feed into")
                .define(BLUE_TOPIC_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "kafka topic to publish the feed into");
    }


}

