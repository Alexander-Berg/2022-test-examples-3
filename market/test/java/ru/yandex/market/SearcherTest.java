package ru.yandex.market;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearcherTest {

    public static final Comparator<Key<Integer>> CMP = new Comparator<Key<Integer>>() {
        @Override
        public int compare(Key<Integer> o1, Key<Integer> o2) {
            return Integer.compare(o1.id(), o2.id());
        }
    };

    private Searcher<Key<Integer>, Ik> ikSearcher = new Searcher<Key<Integer>, Ik>(CMP);

    private Searcher<Integer, Integer> integerSearcher = new Searcher<Integer, Integer>(new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return Integer.compare(o1, o2);
        }
    });

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testIdx() {
        Integer[] sortedKeys = {3, 7, 9};
        assertEquals(-1, integerSearcher.idx(sortedKeys, 0, 1));
        assertEquals(0, integerSearcher.idx(sortedKeys, 0, 3));
        assertEquals(1, integerSearcher.idx(sortedKeys, 0, 7));
        assertEquals(2, integerSearcher.idx(sortedKeys, 0, 9));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 0, 10));

        assertEquals(-2, integerSearcher.idx(sortedKeys, 1, 1));
        assertEquals(-2, integerSearcher.idx(sortedKeys, 1, 3));
        assertEquals(1, integerSearcher.idx(sortedKeys, 1, 7));
        assertEquals(2, integerSearcher.idx(sortedKeys, 1, 9));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 1, 10));

        assertEquals(-3, integerSearcher.idx(sortedKeys, 2, 1));
        assertEquals(-3, integerSearcher.idx(sortedKeys, 2, 3));
        assertEquals(-3, integerSearcher.idx(sortedKeys, 2, 7));
        assertEquals(2, integerSearcher.idx(sortedKeys, 2, 9));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 2, 10));

        assertEquals(-4, integerSearcher.idx(sortedKeys, 3, 1));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 3, 3));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 3, 7));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 3, 9));
        assertEquals(-4, integerSearcher.idx(sortedKeys, 3, 10));
    }

    @Test
    public void testFind() {
        List<Integer> searchKeys = Arrays.asList(1, 2, 3, 5, 7, 10, 11, 100);
        Integer[] sortedKeys = {3, 7, 9};
        List<Integer> found = integerSearcher.find(searchKeys, sortedKeys);
        assertEquals(2, found.size());
        assertEquals(3, found.get(0).intValue());
        assertEquals(7, found.get(1).intValue());

        searchKeys = Arrays.asList(5, 7, 10, 11, 100);
        found = integerSearcher.find(searchKeys, sortedKeys);
        assertEquals(1, found.size());
        assertEquals(7, found.get(0).intValue());

        searchKeys = Arrays.asList(5, 7);
        found = integerSearcher.find(searchKeys, sortedKeys);
        assertEquals(1, found.size());
        assertEquals(7, found.get(0).intValue());

        searchKeys = Arrays.asList(3, 7, 9);
        found = integerSearcher.find(searchKeys, sortedKeys);
        assertEquals(3, found.size());
        assertEquals(3, found.get(0).intValue());
        assertEquals(7, found.get(1).intValue());
        assertEquals(9, found.get(2).intValue());

        searchKeys = Arrays.asList(5);
        found = integerSearcher.find(searchKeys, sortedKeys);
        assertTrue(found.isEmpty());

        searchKeys = Arrays.asList(1, 2, 5, 10, 11, 100);
        found = integerSearcher.find(searchKeys, sortedKeys);
        assertTrue(found.isEmpty());

        searchKeys = Collections.EMPTY_LIST;
        found = integerSearcher.find(searchKeys, sortedKeys);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testFindSearch() {
        List<Integer> searchKeys = Arrays.asList(1, 2, 3, 5, 7, 10, 11, 100);
        Integer[] sortedKeys = {3, 7, 9};

        Searcher.FindListener<Integer> listener = new Searcher.FindListener<>(sortedKeys);
        integerSearcher.search(searchKeys, sortedKeys, listener);
        assertEquals(2, listener.result().size());
        assertEquals(3, listener.result().get(0).intValue());
        assertEquals(7, listener.result().get(1).intValue());
    }

    @Test
    public void testUpdateSearch() {
        List<Integer> searchKeys = Arrays.asList(1, 2, 3, 5, 7, 10, 11, 100);
        Integer[] sortedKeys = {3, 7, 9};

        Searcher.UpdateListener listener = new Searcher.UpdateListener(searchKeys.size());
        integerSearcher.search(searchKeys, sortedKeys, listener);

        int[] expectedPoints = {-1, -1, 0, -2, 1, -4, -4, -4};
        int[] actualPoints = listener.result();
        assertEquals(2, listener.updates());
        assertTrue(Arrays.toString(actualPoints), Arrays.equals(expectedPoints, actualPoints));
    }

    @Test
    public void testSmartUpdate() throws IOException {
        //mix
        List<Ik> updates = Arrays.asList(Ik.of(1), Ik.of(5));
        Ik[] data = new Ik[]{Ik.of(11), Ik.of(3), Ik.of(19)};
        int[] expectedValues = new int[]{1, 3, 5, 19};
        int[] actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1), Ik.of(5));
        data = new Ik[]{Ik.of(11), Ik.of(3), Ik.of(19)};
        expectedValues = new int[]{11, 3, 5, 19};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(11), Ik.of(3), Ik.of(19));
        data = new Ik[]{Ik.of(1), Ik.of(5)};
        expectedValues = new int[]{11, 3, 5, 19};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(11), Ik.of(3), Ik.of(19));
        data = new Ik[]{Ik.of(1), Ik.of(5)};
        expectedValues = new int[]{1, 3, 5, 19};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        //updates only
        updates = Arrays.asList(Ik.of(11), Ik.of(15));
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{11, 15, 7};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(11), Ik.of(15));
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        //inserts only
        updates = Arrays.asList(Ik.of(13), Ik.of(19));
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 13, 5, 7, 19};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(13), Ik.of(19));
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 13, 5, 7, 19};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        //peculiarities
        updates = Arrays.asList();
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList();
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1), Ik.of(5), Ik.of(7));
        data = new Ik[]{};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1), Ik.of(5), Ik.of(7));
        data = new Ik[]{};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1));
        data = new Ik[]{Ik.of(11)};
        expectedValues = new int[]{1};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1));
        data = new Ik[]{Ik.of(11)};
        expectedValues = new int[]{11};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1));
        data = new Ik[]{Ik.of(2)};
        expectedValues = new int[]{1, 2};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(2));
        data = new Ik[]{Ik.of(1)};
        expectedValues = new int[]{1, 2};
        actualValues = transform(ikSearcher.smartUpdate(updates, data, false));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
    }

    @Test
    public void testSmartUpdateArrayCopyTimes() throws IOException {
        final int[] countCopy = {0};
        Searcher<Key<Integer>, Ik> searcher = new Searcher<Key<Integer>, Ik>(CMP) {
            @Override
            protected void copyArray(Object arrayOrig, int idxOrigFrom, Object arrayDest, int idxDestFrom, int count) {
                countCopy[0]++;
                super.copyArray(arrayOrig, idxOrigFrom, arrayDest, idxDestFrom, count);
            }
        };

        List<Ik> updates = Arrays.asList(Ik.of(1), Ik.of(12), Ik.of(6));
        Ik[] data = new Ik[]{Ik.of(10), Ik.of(12), Ik.of(3), Ik.of(15), Ik.of(6)};
        int[] expectedValues = new int[]{10, 1, 12, 3, 15, 6};
        int[] actualValues = transform(searcher.smartUpdate(updates, data, true));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
        assertEquals("extra array copies detected", 1, countCopy[0]);
    }

    @Test
    public void testFailedSimpleUpdateEasy() {
        List<Integer> updates = Arrays.asList(23, 77, 93);
        Integer[] data = new Integer[]{8, 77};
        int[] expectedValues = new int[]{8, 23, 77, 93};
        int[] actualValues = ArrayUtils.toPrimitive(integerSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
    }

    @Test
    public void testFailedSimpleUpdateFull() {
        List<Ik> updates = Arrays.asList(Ik.of(3), Ik.of(7), Ik.of(9));
        Ik[] data = new Ik[]{Ik.of(12), Ik.of(17)};
        int[] expectedValues = new int[]{12, 3, 7, 9};
        int[] actualValues = transform(ikSearcher.smartUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
    }

    @Test
    public void testFailedSmartUpdateEasy() {
        List<Integer> updates = Arrays.asList(14, 20, 65);
        Integer[] data = new Integer[]{65, 76};
        int[] expectedValues = new int[]{14, 20, 65, 76};
        int[] actualValues = ArrayUtils.toPrimitive(integerSearcher.smartUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
    }

    @Test
    public void testFailedSmartUpdateFull() {
        List<Ik> updates = Arrays.asList(Ik.of(2), Ik.of(4), Ik.of(5));
        Ik[] data = new Ik[]{Ik.of(15), Ik.of(16)};
        int[] expectedValues = new int[]{2, 4, 5, 16};
        int[] actualValues = transform(ikSearcher.smartUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
    }

    @Test
    public void testRandomSimpleUpdate() throws IOException {
        long millis = System.currentTimeMillis();
        testRandomUpdate(false);
        System.out.printf("SimpleUpdate time elapsed %.3f s\n", (System.currentTimeMillis() - millis) / 1000.0);
    }

    @Test
    public void testRandomSmartUpdate() throws IOException {
        long millis = System.currentTimeMillis();
        testRandomUpdate(true);
        System.out.printf("SmartUpdate time elapsed %.3f s\n", (System.currentTimeMillis() - millis) / 1000.0);
    }

    private void testRandomUpdate(boolean isSmart) throws IOException {
        int testsCount = 100_000;
        int updatesCount;
        Random rnd = new Random();
        int maxCount = 10;
        int maxValue = 100;
        List<Integer> updates = new ArrayList<>();
        Integer[] data;
        BitSet found = new BitSet(maxValue);
        Set<Integer> expected = new TreeSet<>();

        Path path = Paths.get(System.getProperty("user.dir"), "search.test");
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            int next;
            while (testsCount-- > 0) {
                expected.clear();

                found.clear();
                updates.clear();
                updatesCount = rnd.nextInt(maxCount);
                while (updates.size() != updatesCount) {
                    next = rnd.nextInt(maxValue);
                    if (!found.get(next)) {
                        updates.add(next);
                        expected.add(next);
                        found.set(next);
                    }
                }

                found.clear();
                data = new Integer[rnd.nextInt(maxCount)];
                int i = 0;
                while (i < data.length) {
                    next = rnd.nextInt(maxValue);
                    if (!found.get(next)) {
                        data[i++] = next;
                        expected.add(next);
                        found.set(next);
                    }
                }

                Collections.sort(updates);
                Arrays.sort(data);

                writer.write("[");
                writer.write(Joiner.on(",").join(updates));
                writer.write("]\t[");
                writer.write(Joiner.on(",").join(data));
                writer.write("]\t[");
                writer.write(Joiner.on(",").join(expected));
                writer.write("]\t");

                int[] actualValues = ArrayUtils.toPrimitive(isSmart ?
                        integerSearcher.smartUpdate(updates, data) :
                        integerSearcher.simpleUpdate(updates, data), -1);

                writer.write(Arrays.toString(actualValues));
                writer.write("\n");

                assertTrue(String.format("See %s for problems", path.toString()),
                        Arrays.equals(ArrayUtils.toPrimitive(expected.toArray(new Integer[expected.size()])), actualValues));
            }
            Files.delete(path);
        }
    }

    @Test
    public void testSimpleUpdate() {
        //mix
        List<Ik> updates = Arrays.asList(Ik.of(1), Ik.of(5));
        Ik[] data = new Ik[]{Ik.of(11), Ik.of(3), Ik.of(19)};
        int[] expectedValues = new int[]{1, 3, 5, 19};
        int[] actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(11), Ik.of(3), Ik.of(19));
        data = new Ik[]{Ik.of(1), Ik.of(5)};
        expectedValues = new int[]{11, 3, 5, 19};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        //updates only
        updates = Arrays.asList(Ik.of(11), Ik.of(15));
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{11, 15, 7};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        //inserts only
        updates = Arrays.asList(Ik.of(13), Ik.of(19));
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 13, 5, 7, 19};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        //peculiarities
        updates = Arrays.asList();
        data = new Ik[]{Ik.of(1), Ik.of(5), Ik.of(7)};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1), Ik.of(5), Ik.of(7));
        data = new Ik[]{};
        expectedValues = new int[]{1, 5, 7};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1));
        data = new Ik[]{Ik.of(11)};
        expectedValues = new int[]{1};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));

        updates = Arrays.asList(Ik.of(1));
        data = new Ik[]{Ik.of(2)};
        expectedValues = new int[]{1, 2};
        actualValues = transform(ikSearcher.simpleUpdate(updates, data));
        assertTrue(Arrays.toString(actualValues), Arrays.equals(expectedValues, actualValues));
    }

    private int[] transform(Ik[] values) {
        return ArrayUtils.toPrimitive(Arrays.stream(values).map(key -> key.value).toArray(Integer[]::new));
    }

    public static interface Key<T> {
        public T id();
    }

    public static class Ik implements Key<Integer> {
        private int value;

        private Ik(int value) {
            this.value = value;
        }

        public static Ik of(int value) {
            return new Ik(value);
        }

        @Override
        public Integer id() {
            return value % 10;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}