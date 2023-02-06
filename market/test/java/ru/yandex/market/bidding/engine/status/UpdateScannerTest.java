package ru.yandex.market.bidding.engine.status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Joiner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateScannerTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDoScan() throws Exception {
        int testsCount = 100_000;
        int count;
        Random rnd = new Random();
        int maxCount = 5;
        int maxValue = 100;
        TreeSet<Integer> updates = new TreeSet<>();
        TreeSet<Integer> cached = new TreeSet<>();

        Path path = Paths.get(System.getProperty("user.dir"), "update.status.test");
        System.out.println("Result in " + path.toString());
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            int next;
            while (testsCount-- > 0) {
                updates.clear();
                count = rnd.nextInt(maxCount);
                while (updates.size() != count) {
                    next = rnd.nextInt(maxValue);
                    updates.add(next);
                }

                cached.clear();
                count = rnd.nextInt(maxCount);
                while (cached.size() != count) {
                    next = rnd.nextInt(maxValue);
                    cached.add(next);
                }

                writer.write("[");
                writer.write(Joiner.on(",").join(updates));
                writer.write("]\t[");
                writer.write(Joiner.on(",").join(cached));
                writer.write("]\t[");

                IntegerUpdateStatusStrategy listener = new IntegerUpdateStatusStrategy(updates, cached);
                new UpdateScanner(true).doScan(new ArrayList<>(updates), new ArrayList<>(cached), listener);

                writer.write(Joiner.on(",").join(listener.indices));
                writer.write("]\n");

                assertTrue(String.format("%d - see %s for problems", listener.indices.size(), path.toString()),
                        listener.indices.size() == cached.size());
            }
            Files.delete(path);
        }
    }

    public void testOzon() throws IOException {
        List<String> cachedValues = new ArrayList<>(300_000);
        String line;
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(
                             new GZIPInputStream(
                                     Files.newInputStream(
                                             Paths.get(System.getProperty("user.dir"), "155_names.csv.gz")))))) {
            while ((line = reader.readLine()) != null) {
                cachedValues.add(line);
            }
        }

        List<String> updateValues = new ArrayList<>(300_000);
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(
                             new GZIPInputStream(
                                     Files.newInputStream(
                                             Paths.get(System.getProperty("user.dir"), "155_result.csv.gz")))))) {
            while ((line = reader.readLine()) != null) {
                updateValues.add(line);
            }
        }

        Collections.sort(cachedValues);
        Collections.sort(updateValues);


        StringUpdateStatusStrategy listener = new StringUpdateStatusStrategy(new TreeSet(updateValues), new TreeSet(cachedValues));
        new UpdateScanner(true).doScan(updateValues, cachedValues, listener);

        assertTrue(listener.indices.size() == cachedValues.size());

    }

    static class IntegerUpdateStatusStrategy implements UpdateScannerStrategy<Integer, Integer> {
        private final TreeSet<Integer> updateValues;
        private final TreeSet<Integer> cachedValues;
        private final Set<Integer> indices = new HashSet<>();

        IntegerUpdateStatusStrategy(TreeSet<Integer> updates, TreeSet<Integer> cached) {
            this.updateValues = new TreeSet<>(updates);
            this.cachedValues = cached;
        }


        @Override
        public boolean needTraceUpdate(Integer update) {
            return false;
        }

        @Override
        public boolean needTraceCached(Integer cached) {
            return false;
        }

        @Override
        public int compare(Integer update, Integer cached) {
            return Integer.compare(update, cached);
        }

        @Override
        public void onUpdate(Integer update, Integer cached, int idx, boolean trace) {
            assertTrue(String.valueOf(idx), idx >= 0);
            assertTrue(String.valueOf(idx), idx < cachedValues.size());
            assertEquals(update, cached);
            assertTrue(String.valueOf(update), updateValues.contains(update));
            assertTrue(String.valueOf(idx), indices.add(idx));
        }

        @Override
        public void onAbsent(Integer cached, int idx, boolean trace) {
            assertTrue(String.valueOf(idx), idx >= 0);
            assertTrue(String.valueOf(idx), idx < cachedValues.size());
            assertFalse(String.valueOf(cached), updateValues.contains(cached));
            assertTrue(String.valueOf(idx), indices.add(idx));
        }
    }

    static class StringUpdateStatusStrategy implements UpdateScannerStrategy<String, String> {
        private final TreeSet<String> updateValues;
        private final TreeSet<String> cachedValues;
        private final Set<Integer> indices = new HashSet<>();

        StringUpdateStatusStrategy(TreeSet<String> updates, TreeSet<String> cached) {
            this.updateValues = new TreeSet<>(updates);
            this.cachedValues = cached;
        }


        @Override
        public boolean needTraceUpdate(String update) {
            return false;
        }

        @Override
        public boolean needTraceCached(String cached) {
            return false;
        }

        @Override
        public int compare(String update, String cached) {
            return update.compareTo(cached);
        }

        @Override
        public void onUpdate(String update, String cached, int idx, boolean trace) {
            assertTrue(String.valueOf(idx), idx >= 0);
            assertTrue(String.valueOf(idx), idx < cachedValues.size());
            assertEquals(update, cached);
            assertTrue(String.valueOf(update), updateValues.contains(update));
            assertTrue(String.valueOf(idx), indices.add(idx));
        }

        @Override
        public void onAbsent(String cached, int idx, boolean trace) {
            assertTrue(String.valueOf(idx), idx >= 0);
            assertTrue(String.valueOf(idx), idx < cachedValues.size());
            assertFalse(String.valueOf(cached), updateValues.contains(cached));
            assertTrue(String.valueOf(idx), indices.add(idx));
        }
    }
}