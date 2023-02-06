package ru.yandex.market.mbo.flume.channel;

import com.google.common.collect.ImmutableMap;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelUtils;
import org.apache.flume.event.SimpleEvent;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.flume.yt.YtClientProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author amaslak
 */
public class YtChannelTest {

    private static final String PAYLOAD = "test string";
    private static final String HEADER_KEY = "test key";
    private static final String HEADER_VALUE = "value test";

    private YtChannel channel;

    @Before
    public void prepare() {
        DOMConfigurator.configure(
                getClass().getResource("/mbo-flume-ng/log4j-integration-test-config.xml"));
    }

    @Before
    public void setUp() {
        channel = new YtChannel();
        channel.setName("test-channel-" + UUID.randomUUID());

        // Configure channel
        Map<String, String> config = new HashMap<>();
        config.put(YtChannel.BATCH_SIZE, "10");
        config.put(YtChannel.AGENT_ID, "localhost");
        config.put(YtChannel.HTTP_PROXY, "hahn.yt.yandex.net");
        config.put(YtChannel.YT_CLIENT, "yt");
        config.put(YtChannel.TABLE_PATH, "//home/market/development/mbo/integration_test/mbo-flume-channel");

        System.setProperty(YtClientProvider.RPC_LOGIN, "robot-mbo-dev");
        System.setProperty(YtClientProvider.RPC_CLUSTER, "hahn");
        System.setProperty(YtClientProvider.RPC_THREADS, "4");
        System.setProperty(YtClientProvider.RPC_TIMEOUT, "60000");
        System.setProperty(YtClientProvider.RPC_PING_TIMEOUT, "5000");
        System.setProperty(YtClientProvider.RPC_FAILOVER_TIMEOUT, "1000");

        // replace with actual yt token
        String secretToken = "SECRET";
        System.setProperty(YtClientProvider.RPC_TOKEN, secretToken);
        config.put(YtChannel.TOKEN, secretToken);

        Context context = new Context(config);
        channel.configure(context);
    }

    @Test
    public void putAndTake() {
        List<Event> take0 = ChannelUtils.take(channel, 1);
        Assert.assertEquals(0, take0.size());

        SimpleEvent event = new SimpleEvent();

        event.setBody(PAYLOAD.getBytes());
        event.setHeaders(ImmutableMap.of(HEADER_KEY, HEADER_VALUE));

        ChannelUtils.put(channel, event);

        List<Event> take1 = ChannelUtils.take(channel, 1);
        Assert.assertEquals(1, take1.size());

        Assert.assertEquals(event.getHeaders(), take1.get(0).getHeaders());
        Assert.assertArrayEquals(event.getBody(), take1.get(0).getBody());

        List<Event> take2 = ChannelUtils.take(channel, 1);
        Assert.assertEquals(0, take2.size());
    }

    @Test
    public void channelSize() {
        channel.updateChannelSize();
    }
}
