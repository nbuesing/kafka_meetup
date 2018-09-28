package com.objectpartners.buesing.connector;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.opensky.api.OpenSkyApi;
import org.opensky.model.OpenSkyStates;
import org.opensky.model.StateVector;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class OpenSkyTask extends SourceTask {

    /**
     * {
     *     "time": 1535739820,
     *     "states": [
     *         [
     *             "a2cbcc",
     *             "N28BS   ",
     *             "United States",
     *             1535739649,
     *             1535739649,
     *             -122.5351,
     *             38.1321,
     *             167.64,
     *             false,
     *             31.29,
     *             226.33,
     *             -2.93,
     *             null,
     *             160.02,
     *             null,
     *             false,
     *             0
     *         ]
     */

    private static final Schema KEY_SCHEMA = Schema.STRING_SCHEMA;
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String CHANNEL_FIELD = "channel";

    private BlockingQueue<SourceRecord> queue = null;
    private String topic = null;
    private String blue = null;
    private String red = null;

    private long lastTimestamp;
    private long maxTimestamp;

    @Override
    public String version() {
        return new OpenSkyConnector().version();
    }

    @Override
    public void start(Map<String, String> props) {
        queue = new LinkedBlockingQueue<>();
        topic = props.get(OpenSkyConnector.TOPIC_CONFIG);
        red = props.get(OpenSkyConnector.RED_TOPIC_CONFIG);
        blue = props.get(OpenSkyConnector.BLUE_TOPIC_CONFIG);

        System.out.println("STARTING>>>>");

    }

    public static void main(String[] args) {
        OpenSkyTask t = new OpenSkyTask();


        t.foo();
    }

    private void foo() {
        try {
            OpenSkyApi api = new OpenSkyApi(null, null);
            //OpenSkyStates os = api.getStates(0, null, new OpenSkyApi.BoundingBox(45.8389, 47.8229, 5.9962, 10.5226));
            OpenSkyStates os = api.getStates(0, null, new OpenSkyApi.BoundingBox(24.396308, 49.384358, -124.848974, -66.885444));

            System.out.println("***");
            System.out.println(os.getTime());
            System.out.println(os.getStates().size());
            System.out.println("***");

            Collection<StateVector> c = os.getStates();

            maxTimestamp = 0L;

            //TEMP
            final long timestamp = System.currentTimeMillis();

            c.forEach(vector -> {

                String icao24 = vector.getIcao24();

                int mod = icao24.hashCode() % 2;

                //long timestamp = vector.getLastContact().longValue() * 1000L;

                if (timestamp > maxTimestamp) {
                    maxTimestamp = timestamp;
                }

                if (timestamp > lastTimestamp) {
                    System.out.println("TIMESTAMP " + timestamp);
                    // System.out.println("LAST CONTACT " + (timestamp.longValue() * 1000L));
                    //System.out.println("        SSSS " + System.currentTimeMillis());

                    SourceRecord record = new SourceRecord(null, null, (mod % 2 == 0) ? red : blue, 0, KEY_SCHEMA, icao24, Message.SCHEMA, new Message(vector), timestamp);

                    // System.out.println(record);

                    queue.offer(record);
                } else {
                    System.out.println("OLD");
                }
            });

            lastTimestamp = maxTimestamp;

        } catch (IOException e) {
            System.out.println("***");
            System.out.println(e.getMessage());
            System.out.println("***");
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {

        Thread.sleep(15000L);

        System.out.println("POLL : " + System.currentTimeMillis());

        if (queue.isEmpty()) {
            foo();
        }

        List<SourceRecord> result = new LinkedList<>();
        if (queue.isEmpty()) result.add(queue.take());
        queue.drainTo(result);

        System.out.println("DRAIN : " + result.size());
        return result;
    }

    @Override
    public void stop() {
        queue.clear();
    }

//    class IRCMessageListener extends IRCEventAdapter {
//        @Override
//        public void onPrivmsg(String channel, IRCUser u, String msg) {
//            Message event = new Message(channel, u, msg);
//            //FIXME kafka round robin default partitioner seems to always publish to partition 0 only (?)
//            long ts = event.getInt64("timestamp");
//            Map<String, ?> srcOffset = Collections.singletonMap(TIMESTAMP_FIELD, ts);
//            Map<String, ?> srcPartition = Collections.singletonMap(CHANNEL_FIELD, channel);
//            SourceRecord record = new SourceRecord(srcPartition, srcOffset, topic, KEY_SCHEMA, ts, Message.SCHEMA, event);
//            queue.offer(record);
//        }
//
//        @Override
//        public void onError(String msg) {
//            log.warn("IRC Error: " + msg);
//        }
//    }

}
