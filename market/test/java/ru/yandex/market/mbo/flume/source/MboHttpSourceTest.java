package ru.yandex.market.mbo.flume.source;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.flume.Event;
import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.node.AbstractConfigurationProvider;
import org.apache.flume.node.Application;
import org.apache.flume.node.MaterializedConfiguration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.flume.sink.AbstractJavaSink;
import ru.yandex.market.mbo.http.MboFlumeNg;

/**
 * @author amaslak
 */
public class MboHttpSourceTest {

    private static final int TIMEOUT_SECONDS = 30;

    private MaterializedConfiguration prepare() {
        ImmutableMap<String, String> conf = ImmutableMap.<String, String>builder()
                .put("mbo-flume-ng.sources", "main")
                .put("mbo-flume-ng.channels", "chan")
                .put("mbo-flume-ng.sinks", "out")

                .put("mbo-flume-ng.sources.main.type", "ru.yandex.market.mbo.flume.source.MboHttpSource")
                .put("mbo-flume-ng.sources.main.port", "0")
                .put("mbo-flume-ng.sources.main.threads", "32")
                .put("mbo-flume-ng.sources.main.channels", "chan")

                .put("mbo-flume-ng.channels.chan.type", "memory")
                .put("mbo-flume-ng.channels.chan.capacity", "100")

                .put("mbo-flume-ng.sinks.out.type", TestHelperSink.class.getName())
                .put("mbo-flume-ng.sinks.out.channel", "chan")

                .build();

        FlumeConfiguration flumeConfiguration = new FlumeConfiguration(conf);

        List<String> errors = flumeConfiguration.getConfigurationErrors().stream()
                .map(e -> e.getComponentName() + e.getKey())
                .collect(Collectors.toList());
        Assert.assertTrue(errors.isEmpty());

        String name = "mbo-flume-ng";
        AbstractConfigurationProvider p = new AbstractConfigurationProvider(name) {
            @Override
            protected FlumeConfiguration getFlumeConfiguration() {
                return flumeConfiguration;
            }
        };
        return p.getConfiguration();
    }


    /**
     * Test chained configuration: http source -> channel -> null sink.
     */
    @Test
    public void confPing() throws InterruptedException, InvalidProtocolBufferException {

        Application app = new Application();
        MaterializedConfiguration materializedConfiguration = prepare();
        app.handleConfigurationEvent(materializedConfiguration);
        app.start();

        MboHttpSource main = (MboHttpSource) materializedConfiguration.getSourceRunners()
                .get("main")
                .getSource();

        MboFlumeNg.ModelUpdateEvent sourceEvent = MboFlumeNg.ModelUpdateEvent.newBuilder()
                .setId(1L)
                .setCategoryId(2L)
                .setModifiedTimestamp(System.currentTimeMillis())
                .build();

        MboFlumeNg.ReindexRequest request = MboFlumeNg.ReindexRequest.newBuilder()
                .addEvent(sourceEvent)
                .build();

        main.reindex(request);

        TestHelperSink.TRANSACTION_COUNTER.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Event event = TestHelperSink.EVENT_LIST.get(0);
        MboFlumeNg.ModelUpdateEvent sinkEvent =
                MboFlumeNg.ModelUpdateEvent.PARSER.parseFrom(event.getBody());

        Assert.assertEquals(sourceEvent.getCategoryId(), sinkEvent.getCategoryId());
        Assert.assertEquals(sourceEvent.getId(), sinkEvent.getId());

        String expectedModifiedDate = event.getHeaders().get("expected_modified_date");
        Long expectedModifiedTimestamp = Long.parseLong(expectedModifiedDate);

        Assert.assertEquals((Long) sourceEvent.getModifiedTimestamp(), expectedModifiedTimestamp);

        app.stop();
    }


    /**
     * Test chained configuration: http source -> channel -> null sink.
     */
    @Test
    public void confSource() {

        Application app = new Application();
        MaterializedConfiguration materializedConfiguration = prepare();
        app.handleConfigurationEvent(materializedConfiguration);
        app.start();

        MboHttpSource main = (MboHttpSource) materializedConfiguration.getSourceRunners()
                .get("main")
                .getSource();

        MonitoringResult result = main.ping();

        Assert.assertEquals(MonitoringResult.OK.getStatus(), result.getStatus());
        Assert.assertEquals(MonitoringResult.OK.getMessage(), result.getMessage());

        app.stop();
    }

    /**
     * Sink intended for unit test validation of output events.
     *
     * @author amaslak
     */
    @Ignore
    public static class TestHelperSink extends AbstractJavaSink {

        private static final List<Event> EVENT_LIST = new CopyOnWriteArrayList<>();
        private static final CountDownLatch TRANSACTION_COUNTER = new CountDownLatch(1);

        @Override
        protected void doProcessEvents(List<Event> batch) {
            EVENT_LIST.addAll(batch);
            TRANSACTION_COUNTER.countDown();
        }

        public SinkCounter getSinkCounter() {
            return sinkCounter;
        }
    }
}
