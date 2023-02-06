package ru.yandex.market.mbo.flume.channelselectors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.SimpleEvent;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.flume.channel.ChannelMock;
import ru.yandex.market.mbo.http.MboFlumeNg;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelChannelSelectorTest {

    private static final String ID_HEADER = "id";
    private static final int TEST_SET_SIZE = 10_000;

    @Test
    public void testChannelSelect() {
        ModelChannelSelector selector = new ModelChannelSelector();

        selector.setName("МodelChannelSelector");

        Channel ch1 = new ChannelMock();
        Channel ch2 = new ChannelMock();

        selector.setChannels(Arrays.asList(ch1, ch2));
        selector.configure(new Context());

        Assert.assertEquals(2, selector.getNumChannels());
        Assert.assertEquals(Arrays.asList(ch1, ch2), selector.getAllChannels());

        Random random = new Random(0);

        List<Event> events = generateEvents(random);

        Multimap<Channel, Event> multimap = ArrayListMultimap.create();
        for (Event event : events) {
            List<Channel> requiredChannels = selector.getRequiredChannels(event);
            Assert.assertEquals(1, requiredChannels.size());

            Channel channel = requiredChannels.get(0);
            multimap.put(channel, event);
        }

        Assert.assertEquals(new HashSet<>(Arrays.asList(ch1, ch2)), multimap.keySet());

        Function<Event, Long> getIdHeader = ModelChannelSelectorTest::getIdHeader;
        Set<Long> ids1 = multimap.get(ch1).stream().map(getIdHeader).collect(Collectors.toSet());
        Set<Long> ids2 = multimap.get(ch2).stream().map(getIdHeader).collect(Collectors.toSet());

        // same model_id never passed to both channels
        Assert.assertTrue(Sets.intersection(ids1, ids2).isEmpty());

        // distribution is close to even, diff is less than 10%
        int size1 = multimap.get(ch1).size();
        int size2 = multimap.get(ch2).size();
        Assert.assertTrue(Math.abs(size1 - size2) * 10 < TEST_SET_SIZE);
    }

    public static List<Event> generateEvents(Random random) {
        return random.ints(TEST_SET_SIZE, 1, TEST_SET_SIZE / 10)
                    .mapToObj(i -> MboFlumeNg.ModelUpdateEvent.newBuilder()
                            .setId(i)
                            .setModifiedTimestamp(random.nextLong())
                            .setCategoryId(random.nextLong())
                            .build())
                    .map(e -> {
                        SimpleEvent event = new SimpleEvent();
                        event.setHeaders(ImmutableMap.of(ID_HEADER, Long.toString(e.getId())));
                        event.setBody(e.toByteArray());
                        return event;
                    })
                    .collect(Collectors.toList());
    }

    public static Long getIdHeader(Event event) {
        return Long.valueOf(event.getHeaders().get(ID_HEADER));
    }

    @Test
    public void testByteConversion() {
        Random random = new Random(0);
        random.longs(TEST_SET_SIZE).forEach(i -> {
            long j = ModelChannelSelector.toLong(ModelChannelSelector.toBytes(i));
            Assert.assertEquals(i, j);
        });
    }
}
