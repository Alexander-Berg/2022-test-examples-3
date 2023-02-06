package ru.yandex.market.bidding.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 05.10.15
 * Time: 21:09
 */
public class BigDataTest {

    @Test
    public void testSort() throws Exception {
        List<Integer> sample = new ArrayList<>();
        Random random = new Random();
        int chunkSize = 0;
        for (int i = 0; i < 100; i++) {
            int count = random.nextInt(90) + 10;
            while (count-- > 0) {
                sample.add(random.nextInt(100));
            }
            List<Integer> original = new ArrayList<>(sample);
            List<Integer> expected = new ArrayList<>(sample);
            expected.sort(Comparator.<Integer>naturalOrder());
            final InMemoryIntDataSortStrategy strategy = new InMemoryIntDataSortStrategy(sample);
            BigData.sort(strategy, key -> key, ++chunkSize % 5 + 5);
            assertEquals(original.toString(), expected, sample);
            sample.clear();
        }
    }

    private static class InMemoryIntDataSortStrategy implements BigData.SortStrategy<Integer> {

        private final List<Integer> input;
        private final List<IntOutput> buffers = new ArrayList<>();
        private final List<Integer> result = new ArrayList<>();

        private InMemoryIntDataSortStrategy(List<Integer> input) {
            this.input = input;
        }

        @Override
        public BigData.Input<Integer> getInput() {
            return new IntInput(input);
        }

        @Override
        public List<BigData.Input<Integer>> getBuffers() {
            return buffers.stream().map(output -> output.toInput()).collect(Collectors.toList());
        }

        @Override
        public BigData.Output<Integer> createResult() {
            return new IntOutput(result);
        }

        @Override
        public BigData.Output<Integer> createBuffer() {
            final IntOutput buffer = new IntOutput(new ArrayList<>());
            buffers.add(buffer);
            return buffer;
        }

        @Override
        public void commit() {
            input.clear();
            input.addAll(result);
        }

        @Override
        public void dispose() {
            buffers.clear();
        }
    }

    private static class IntInput implements BigData.Input<Integer> {

        private final List<Integer> data;

        private IntInput(List<Integer> data) {
            this.data = data;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public Iterator<Integer> iterator() {
            return data.iterator();
        }
    }

    private static class IntOutput implements BigData.Output<Integer> {

        private final List<Integer> data;

        private IntOutput(List<Integer> data) {
            this.data = data;
        }

        @Override
        public void write(Integer value) {
            data.add(value);
        }

        @Override
        public void close() throws IOException {
        }

        public IntInput toInput() {
            return new IntInput(data);
        }
    }
}