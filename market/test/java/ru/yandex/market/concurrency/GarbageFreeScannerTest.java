package ru.yandex.market.concurrency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GarbageFreeScannerTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testScan() {
        Integer[] source = {1, 2, 3, 4, 5};
        Integer[] buffer = new Integer[2];
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        IntegerScanner scanner = new IntegerScanner(source, buffer, lock);
        List<Integer> result = new ArrayList<>();
        while (scanner.hasNext()) {
            result.add(scanner.next());
        }
        assertTrue(Arrays.equals(source, result.toArray()));

        source = new Integer[]{1};
        scanner = new IntegerScanner(source, buffer, lock);
        result = new ArrayList<>();
        while (scanner.hasNext()) {
            result.add(scanner.next());
        }
        assertTrue(Arrays.equals(source, result.toArray()));

        source = new Integer[1000];
        buffer = new Integer[100];
        Random rnd = new Random();
        for (int i = 0; i < source.length; i++) {
            source[i] = rnd.nextInt(1000);
        }
        scanner = new IntegerScanner(source, buffer, lock);
        result = new ArrayList<>();
        scanner.trace();
        while (scanner.hasNext()) {
            result.add(scanner.next());
        }
        scanner.trace();
        assertEquals(source.length, result.size());
        assertTrue(scanner.trace.toString(), Arrays.equals(source, result.toArray()));
    }

    @Test
    public void testEmpty() {
        Integer[] source = {};
        Integer[] buffer = new Integer[2];
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        final IntegerScanner scanner = new IntegerScanner(source, buffer, lock);
        List<Integer> result = new ArrayList<>();
        for (Integer i : new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return scanner;
            }
        }) {
            result.add(i);
        }
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOdd() {
        Integer[] source = {1, 2, 3, 4, 5, 6, 7};
        Integer[] buffer = new Integer[2];
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        final IntegerScanner scanner = new IntegerScanner(source, buffer, lock);
        List<Integer> result = new ArrayList<>();
        for (Integer i : new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return scanner;
            }
        }) {
            result.add(i);
        }
        assertTrue(Arrays.equals(source, result.toArray()));
    }

    @Test
    public void testEven() {
        Integer[] source = {1, 2, 3, 4};
        Integer[] buffer = new Integer[2];
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        final IntegerScanner scanner = new IntegerScanner(source, buffer, lock);
        List<Integer> result = new ArrayList<>();
        for (Integer i : new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return scanner;
            }
        }) {
            result.add(i);
        }
        assertTrue(Arrays.equals(source, result.toArray()));
    }

    protected static class IntegerScanner extends GarbageFreeScanner<Integer> {
        private StringBuilder trace = new StringBuilder();

        public IntegerScanner(Integer[] source, Integer[] buffer, ReentrantReadWriteLock lock) {
            super(source, buffer, lock);
        }

        @Override
        protected void fill() {
            super.fill();
            trace(trace.append("fill - ")).append("\n");
        }

        protected void trace() {
            super.trace(trace).append("\n");
        }
    }
}