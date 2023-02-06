package ru.yandex.market.mbo.flume.channelselectors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.flume.channel.ChannelMock;

public class ReplicatingModelChannelSelectorTest {

    private static final String ID_HEADER = "id";

    private static final int SEED = 4;

    @Test
    public void testChannelSelect() {
        ReplicatingModelChannelSelector selector = new ReplicatingModelChannelSelector();

        selector.setName("МodelChannelSelector");

        Channel ch1 = new ChannelMock("ch1");
        Channel ch2 = new ChannelMock("ch2");
        Channel ch3 = new ChannelMock("ch3");
        Channel ch4 = new ChannelMock("ch4");

        selector.setChannels(Arrays.asList(ch1, ch2, ch3, ch4));
        Context context = new Context();
        context.put(ReplicatingModelChannelSelector.CHANNELS_1, "ch1 ch2");
        context.put(ReplicatingModelChannelSelector.CHANNELS_2, "ch3 ch4");
        selector.configure(context);

        Assert.assertEquals(2, selector.getNumChannels());
        Assert.assertEquals(Arrays.asList(ch1, ch2, ch3, ch4), selector.getAllChannels());

        Random random = new Random(SEED);
        List<Event> events = ModelChannelSelectorTest.generateEvents(random);

        Multimap<Channel, Event> multimap = ArrayListMultimap.create();
        for (Event event : events) {
            List<Channel> requiredChannels = selector.getRequiredChannels(event);
            Assert.assertEquals(2, requiredChannels.size());

            multimap.put(requiredChannels.get(0), event);
            multimap.put(requiredChannels.get(1), event);
        }

        Assert.assertEquals(new HashSet<>(Arrays.asList(ch1, ch2, ch3, ch4)), multimap.keySet());

        Function<Event, Long> getIdHeader = ModelChannelSelectorTest::getIdHeader;
        Set<Long> ids1 = multimap.get(ch1).stream().map(getIdHeader).collect(Collectors.toSet());
        Set<Long> ids2 = multimap.get(ch2).stream().map(getIdHeader).collect(Collectors.toSet());
        Set<Long> ids3 = multimap.get(ch3).stream().map(getIdHeader).collect(Collectors.toSet());
        Set<Long> ids4 = multimap.get(ch4).stream().map(getIdHeader).collect(Collectors.toSet());

        // same model_id never passed to both channels in group
        Assert.assertTrue(Sets.intersection(ids1, ids2).isEmpty());
        Assert.assertTrue(Sets.intersection(ids3, ids4).isEmpty());

        // each model_id passed to both groups
        Assert.assertEquals(ids1, ids3);
        Assert.assertEquals(ids2, ids4);
    }

    private Long getIdHeader(Event event) {
        return Long.valueOf(event.getHeaders().get(ID_HEADER));
    }

}
