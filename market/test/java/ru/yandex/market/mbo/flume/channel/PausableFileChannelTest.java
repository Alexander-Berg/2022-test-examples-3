package ru.yandex.market.mbo.flume.channel;

import java.util.Collections;
import java.util.HashMap;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.SimpleEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


/**
 * @author moskovkin@yandex-team.ru
 * @since 16.08.17
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PausableFileChannelTest {
    public static final String TIMESTAMP_HEADER = "timeToCome";
    public static final long PAUSE_TIME = 1000;

    private Event event = new SimpleEvent();

    private PausableFileChannel channel = new PausableFileChannel();

    @Before
    public void setUp() throws Exception {
        // Configure channel
        HashMap<String, String> config = new HashMap<>();
        config.put(PausableFileChannel.DELEGATE_CHANNEL_CLASS_PARAM, ChannelMock.class.getName());
        config.put(PausableFileChannel.PAUSE_TIME_PARAM, Long.toString(PAUSE_TIME));
        config.put(PausableFileChannel.TIMESTAMP_HEADER_PARAM, TIMESTAMP_HEADER);
        Context context = new Context(config);
        channel.configure(context);

        // Add test events to underlying delegate
        ((ChannelMock) channel.getDelegate()).addEvents(Collections.singleton(event));
    }

    @Test
    public void configureTest() throws Exception {
        Assert.assertEquals(channel.getPauseTime(), PAUSE_TIME);
        Assert.assertEquals(channel.getTimestampHeader(), TIMESTAMP_HEADER);
        Assert.assertTrue("Incorrect delegate class", channel.getDelegate() instanceof ChannelMock);
    }

    @Test
    public void takeIsBlockingOnNewEventsTest() throws Exception {
        // Fresh event - should block
        long eventTimestamp = System.currentTimeMillis();
        event.getHeaders().put(TIMESTAMP_HEADER, String.valueOf(eventTimestamp));

        long before = System.currentTimeMillis();
        channel.take();
        long after = System.currentTimeMillis();

        Assert.assertTrue("Channel not paused correctly"
            + " eventTimestamp: "  + eventTimestamp
            + " before: " + before
            + " after: " + after
            + " PAUSE_TIME: " + PAUSE_TIME,
        after >= eventTimestamp + PAUSE_TIME);
    }

    @Test
    public void takeNotBlockingOnOldEventsTest() throws Exception {
        // Old event should not block
        long eventTimestamp = System.currentTimeMillis() - PAUSE_TIME * 2;
        event.getHeaders().put(TIMESTAMP_HEADER, String.valueOf(eventTimestamp));

        long before = System.currentTimeMillis();
        channel.take();
        long after = System.currentTimeMillis();

        Assert.assertTrue("Channel was paused, but should not"
                + " eventTimestamp: " + eventTimestamp
                + " before: " + before
                + " after: " + after
                + " PAUSE_TIME: " + PAUSE_TIME,
        after < before + PAUSE_TIME);
    }
}
