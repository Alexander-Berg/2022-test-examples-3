package ru.yandex.direct.dbutil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

public class CollectionUtilsWeightedShuffleTest {
    private static final int REPEAT_COUNT = 10000;
    private static final double EPS = 0.01;

    private static final List<Integer> COLL = Arrays.asList(
            0,
            1,
            2,
            3,
            4);
    private static final ToDoubleFunction<Integer> WEIGHTER = i -> i;
    private static final double TOTAL_WEIGHT = 1.0 + 2.0 + 3.0 + 4.0;

    private static final Map<Integer, Double> EXPECTED_PROBS = ImmutableMap.of(
            0, 0.0,
            1, 1.0 / TOTAL_WEIGHT,
            2, 2.0 / TOTAL_WEIGHT,
            3, 3.0 / TOTAL_WEIGHT,
            4, 4.0 / TOTAL_WEIGHT);

    @Test
    public void testPropabilities() {
        Map<Integer, Integer> hist = new HashMap<>(REPEAT_COUNT);
        Random random = new Random(0);

        for (int i = 0; i < REPEAT_COUNT; i++) {
            List<Integer> shuffled = CollectionUtils.weightedShuffle(random, COLL, WEIGHTER);
            Integer firstElement = shuffled.get(0);
            hist.put(firstElement, hist.getOrDefault(firstElement, 0) + 1);
        }

        for (Integer element : COLL) {
            double actualProb = (double) hist.getOrDefault(element, 0) / REPEAT_COUNT;
            double expectedProb = EXPECTED_PROBS.get(element);

            assertThat(Math.abs(actualProb - expectedProb), lessThan(EPS));
        }
    }
}
