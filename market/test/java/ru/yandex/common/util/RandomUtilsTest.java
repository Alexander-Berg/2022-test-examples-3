package ru.yandex.common.util;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.date.TimerUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static ru.yandex.common.util.collections.CollectionFactory.newList;

/**
 * @author ssimonchik@yandex-team.ru
 */
public class RandomUtilsTest extends TestCase {

    // in testing purpose
    interface RandomSampler<T> {
        List<T> randomSample(final List<T> list, int count);
        String getName();
    }

    static class SampleWithModification<T> implements RandomSampler<T> {
        public List<T> randomSample(List<T> list, int count) {
            return RandomUtils.randomSampleWithoutRepeatsWithModification(list, count);
        }

        public String getName() {
            return "with-modification";
        }
    }

    static class FloydSample<T> implements RandomSampler<T> {
        public List<T> randomSample(List<T> list, int count) {
            return RandomUtils.randomSampleWithoutRepeatsFloyd(list, count);
        }

        public String getName() {
            return "Floyd";
        }
    }

    static class CopySample<T> implements RandomSampler<T> {
        public List<T> randomSample(List<T> list, int count) {
            return RandomUtils.randomSampleWithoutRepeatsCopy(list, count);
        }

        public String getName() {
            return "Copy";
        }
    }

    private void checkCorrectness(RandomSampler<Integer> sampler) {
        long t;
        
        System.out.println("Checking that algorithm '" + sampler.getName() + "' does not produce duplicates...");
        t = System.currentTimeMillis();
        List<Integer> xss = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        for (int s = 1; s < xss.size(); ++s) {
            for (int i = 0; i < 100000; ++i) {
                List<Integer> r = sampler.randomSample(xss, s);
                boolean[] f = new boolean[xss.size()];
                for (int x : r) {
                    if (f[x]) {
                        throw new AssertionError(r);
                    } else {
                        f[x] = true;
                    }
                }
            }
        }
        System.out.println("Done in " + TimerUtils.pastMillisWithMetric(t));

        // Check items equiprobability
        System.out.println("Checking that algorithm '" + sampler.getName() + "' " +
                           "produces all items of source list equiprobably...");
        t = System.currentTimeMillis();
        List<Integer> xs = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        int[] freq = new int[10];
        for (int i = 0; i < 100000; ++i) {
            List<Integer> s = sampler.randomSample(xs, 3);

            for (int v : s) freq[v]++;
        }
        System.out.println("Done in " + TimerUtils.pastMillisWithMetric(t));

        for (int i = 0; i < 10; ++i) {
            System.out.println(i + " " + freq[i]);
            assertTrue(Math.abs(freq[i] - 30000) < 2000);
        }

        // Check result permutations equiprobability
        System.out.println("Checking that algorithm '" + sampler.getName() + "' " +
                           "produces all permutations of result equiprobably...");
        t = System.currentTimeMillis();
        xs = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        int[] ind = new int[10];
        for (int i = 0; i < 100000; ++i) {
            List<Integer> s = sampler.randomSample(xs, 3);

            for (int j = 0; j < s.size(); j++) {
                ind[s.get(j)]+=j;
            }
        }
        System.out.println("Done in " + TimerUtils.pastMillisWithMetric(t));

        for (int i = 0; i < 10; ++i) {
            System.out.println(i + " " + ind[i]);
            assertTrue(Math.abs(freq[i] - 30000) < 2000);
        }

        // Check for no crash in some edge cases
        assertEquals(3, sampler.randomSample(Arrays.asList(1, 2, 3), 10).size());
        assertEquals(0, sampler.randomSample(Arrays.asList(1, 2, 3, 4), 0).size());
        assertEquals(1, sampler.randomSample(Arrays.asList(1, 2, 3, 4), 1).size());
        assertEquals(2, sampler.randomSample(Arrays.asList(1, 2, 3, 4), 2).size());
        assertEquals(3, sampler.randomSample(Arrays.asList(1, 2, 3, 4), 3).size());
        assertEquals(4, sampler.randomSample(Arrays.asList(1, 2, 3, 4), 4).size());
        assertEquals(0, sampler.randomSample(Arrays.<Integer>asList(), 10).size());
        assertEquals(1, sampler.randomSample(Arrays.asList(1), 10).size());
        assertEquals(2, sampler.randomSample(Arrays.asList(1, 2), 10).size());
    }

    public void testFloydCorrectness() {
        checkCorrectness(new FloydSample<Integer>());
    }

    public void testSampleWithModificationCorrectness() {
        checkCorrectness(new SampleWithModification<Integer>());
    }

    public void testCopyCorrectness() {
        checkCorrectness(new CopySample<Integer>());
    }

    public void testPerformance() {
        List<RandomSampler<Integer>> samplers = CollectionFactory.list(
            new FloydSample<Integer>(),
            new SampleWithModification<Integer>(),
            new CopySample<Integer>()
        );
        List<Integer> big = newList();
        final int LIST_SIZE = 10000;
        for (int i = 0; i < LIST_SIZE; ++i) big.add(i);
        for (RandomSampler<Integer> sampler : samplers) {
            // warm up
            int res = 0;
            for (int i = 0; i < 1000; i++) {
                List<Integer> randList = sampler.randomSample(big, 5);
                res += randList.get(0);
            }
            final long startTimeMillis = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                List<Integer> randList = sampler.randomSample(big, LIST_SIZE / 10);
                res += randList.get(0);
            }
            System.out.println("'" + sampler.getName() + "' done in " +
                               TimerUtils.pastMillisWithMetric(startTimeMillis) +
                               " res=" + res);
        }
    }

    @Test
    public void testNextIntInRange() {
        int[][] ranges = new int[][] {
                {1, 100},
                {-100, 50},
                {50, 100},
                {-100, -10},
        };
        for (int[] range : ranges) {
            checkRange(range[0], range[1]);
        }
    }

    private void checkRange(int min, int max) {
        Random r = new Random(238);
        int[] fs = new int[max - min + 1];
        final int TESTS = 1000000;
        for (int i = 0; i < TESTS; i++) {
            int v = RandomUtils.nextIntInRange(r, min, max);
            Assert.assertTrue(min <= v && v <= max);
            fs[v - min]++;
        }
        double expected = TESTS / fs.length;
        for (int f : fs) {
            double p = Math.min(f, expected) / Math.max(f, expected);
            Assert.assertTrue("p=" + p, p > 0.95);
        }
    }
}
