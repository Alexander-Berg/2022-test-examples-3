package ru.yandex.market.mbo.flume.source;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.flume.Event;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.node.AbstractConfigurationProvider;
import org.apache.flume.node.Application;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.flume.Register;
import ru.yandex.market.mbo.flume.sink.AbstractJavaSink;

/**
 * @author amaslak
 */
public class SinkSourceTest {

    private static final int TIMEOUT_SECONDS = 30;

    /**
     * Single source-channel-sink group with sink connected back to source via SourceSink-SharedSource pair.
     */
    @Test
    public void testLoopConfiguration() {

        String sinkSourceName = "SinkSourceTest_01";

        ImmutableMap<String, String> conf = ImmutableMap.<String, String>builder()
                .put("mbo-flume-ng.sources", "main")
                .put("mbo-flume-ng.channels", "main_chan_01")
                .put("mbo-flume-ng.sinks", "main_sink_01")

                .put("mbo-flume-ng.sources.main.type", "ru.yandex.market.mbo.flume.source.SharedSource")
                .put("mbo-flume-ng.sources.main.sourceName", sinkSourceName)
                .put("mbo-flume-ng.sources.main.threads", "1")
                .put("mbo-flume-ng.sources.main.channels", "main_chan_01")

                .put("mbo-flume-ng.channels.main_chan_01.type", "memory")
                .put("mbo-flume-ng.channels.main_chan_01.capacity", "100")

                .put("mbo-flume-ng.sinks.main_sink_01.type", "ru.yandex.market.mbo.flume.sink.SourceSink")
                .put("mbo-flume-ng.sinks.main_sink_01.sourceName", sinkSourceName)
                .put("mbo-flume-ng.sinks.main_sink_01.channel", "main_chan_01")

                .build();
        FlumeConfiguration configuration = new FlumeConfiguration(conf);

        List<String> errors = configuration.getConfigurationErrors().stream()
                .map(e -> e.getComponentName() + e.getKey())
                .collect(Collectors.toList());

        Assert.assertTrue(errors.isEmpty());
    }

    /**
     * Test chained configuration: main -> channel -> loopback sink -> loopback source -> channel -> null sink.
     */
    @Test
    public void confSourceSink() throws InterruptedException {

        String mainSourceName = "SinkSourceTest_01";
        String sinkSourceName = "SinkSourceTest_02";

        ImmutableMap<String, String> conf = ImmutableMap.<String, String>builder()
                .put("mbo-flume-ng.sources", "main loopback")
                .put("mbo-flume-ng.channels", "chan_01 chan_02")
                .put("mbo-flume-ng.sinks", "loopback_sink out_sink")

                .put("mbo-flume-ng.sources.main.type", "ru.yandex.market.mbo.flume.source.SharedSource")
                .put("mbo-flume-ng.sources.main.sourceName", mainSourceName)
                .put("mbo-flume-ng.sources.main.channels", "chan_01")

                .put("mbo-flume-ng.sources.loopback.type", "ru.yandex.market.mbo.flume.source.SharedSource")
                .put("mbo-flume-ng.sources.loopback.sourceName", sinkSourceName)
                .put("mbo-flume-ng.sources.loopback.channels", "chan_02")

                .put("mbo-flume-ng.channels.chan_01.type", "memory")
                .put("mbo-flume-ng.channels.chan_01.capacity", "100")

                .put("mbo-flume-ng.channels.chan_02.type", "memory")
                .put("mbo-flume-ng.channels.chan_02.capacity", "100")

                .put("mbo-flume-ng.sinks.loopback_sink.type", "ru.yandex.market.mbo.flume.sink.SourceSink")
                .put("mbo-flume-ng.sinks.loopback_sink.sourceName", sinkSourceName)
                .put("mbo-flume-ng.sinks.loopback_sink.channel", "chan_01")

                .put("mbo-flume-ng.sinks.out_sink.type", TestHelperSink.class.getName())
                .put("mbo-flume-ng.sinks.out_sink.channel", "chan_02")

                .build();

        FlumeConfiguration configuration = new FlumeConfiguration(conf);

        List<String> errors = configuration.getConfigurationErrors().stream()
                .map(e -> e.getComponentName() + e.getKey())
                .collect(Collectors.toList());
        Assert.assertTrue(errors.isEmpty());

        String name = "mbo-flume-ng";
        AbstractConfigurationProvider p = new AbstractConfigurationProvider(name) {
            @Override
            protected FlumeConfiguration getFlumeConfiguration() {
                return configuration;
            }
        };

        Application app = new Application();
        app.handleConfigurationEvent(p.getConfiguration());
        app.start();

        EmbeddedSource source = Register.getEmbeddedSource(mainSourceName);

        SimpleEvent event = new SimpleEvent();
        source.put(event);

        TestHelperSink.TRANSACTION_COUNTER.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertEquals(Collections.singletonList(event), TestHelperSink.EVENT_LIST);

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
