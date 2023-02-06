package ru.yandex.market.crm.campaign.services.segments;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentAlgorithmPart;
import ru.yandex.market.crm.core.domain.segment.SegmentGroupPart;

import static ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType.Channel.EMAIL;


public class SegmentUtilsTest {
    @Test
    public void getSubscriptionTypesForEmptySegment() {
        Segment segment = new Segment();
        SegmentGroupPart config = new SegmentGroupPart();
        config.setParts(Collections.emptyList());
        segment.setConfig(config);

        Set<String> actualTypes = SegmentUtils.getSubscriptionTypes(segment, EMAIL);
        Assert.assertEquals(Collections.emptySet(), actualTypes);
    }

    @Test
    public void getSubscriptionTypesForSegmentWithOneSubscriptionCondition() {
        Segment segment = new Segment();
        SegmentAlgorithmPart config = new SegmentAlgorithmPart();
        config.setAlgorithmId("subscribed");
        config.setProperties(ImmutableMap.of("subscription_types", Arrays.asList("ADVERTISING")));
        segment.setConfig(config);

        Set<String> actualTypes = SegmentUtils.getSubscriptionTypes(segment, EMAIL);
        Assert.assertEquals(Collections.singleton("ADVERTISING"), actualTypes);
    }

    @Test
    public void getSubscriptionTypesForComplexConfig() {
        Segment segment = new Segment();
        SegmentGroupPart config = new SegmentGroupPart();

        SegmentAlgorithmPart part1 = new SegmentAlgorithmPart();
        part1.setAlgorithmId("subscribed");
        part1.setProperties(ImmutableMap.of("subscription_types", Arrays.asList("ADVERTISING", "STORE_ADVERTISING")));

        SegmentAlgorithmPart part2 = new SegmentAlgorithmPart();
        part2.setAlgorithmId("wishlist");
        part2.setProperties(new HashMap<>());

        SegmentGroupPart part3 = new SegmentGroupPart();
        part3.setParts(
                Arrays.asList(
                        new SegmentGroupPart().setParts(
                                Arrays.asList(part1)
                        )
                )
        );

        config.setParts(Arrays.asList(part1, part2, part3));
        segment.setConfig(config);

        Set<String> expectedTypes = new HashSet<>() {{
            add("ADVERTISING");
            add("STORE_ADVERTISING");
        }};
        Set<String> actualTypes = new HashSet<>(SegmentUtils.getSubscriptionTypes(segment, EMAIL));

        Assert.assertEquals(expectedTypes, actualTypes);
    }
}
