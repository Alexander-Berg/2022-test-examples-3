package ru.yandex.market.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 8/18/15
 * Time: 4:06 PM
 */
public class TimeLineHistogramTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSimpleHistogram() throws Exception {
        int[] updates = {-1, 0, -2, 1, 2, 1, 9, 10, 11, 12};
        TimeLineHistogram tlh = new TimeLineHistogram(new TimeLine(1, TimeUnit.SECONDS, 10));
        for (int update : updates) {
            tlh.update(update);
        }
        final List<Pair<Integer, Integer>> histogram = tlh.result();
        String msg = Joiner.on(",").join(histogram);
        assertEquals(msg, 6, histogram.size());
        List<Pair<Integer, Integer>> expected = Arrays.asList(
                Pair.of(0, 2),
                Pair.of(1, 1),
                Pair.of(2, 2),
                Pair.of(3, 1),
                Pair.of(10, 1),
                Pair.of(11, 3)
        );
        assertEquals(msg, expected, histogram);
    }

    @Test
    public void testMinutesHistogram() throws Exception {
        int[] updates = {-70, -50, 0, 30, 59, 60, 120, 125, 355, 360, 370};
        TimeLineHistogram tlh = new TimeLineHistogram(new TimeLine(1, TimeUnit.MINUTES, 6));
        for (int update : updates) {
            tlh.update(update);
        }
        final List<Pair<Integer, Integer>> histogram = tlh.result();
        String msg = Joiner.on(",").join(histogram);
        assertEquals(msg, 6, histogram.size());
        List<Pair<Integer, Integer>> expected = Arrays.asList(
                Pair.of(0, 2),
                Pair.of(1, 3),
                Pair.of(2, 1),
                Pair.of(3, 2),
                Pair.of(6, 1),
                Pair.of(7, 2)
        );
        assertEquals(msg, expected, histogram);
    }
}