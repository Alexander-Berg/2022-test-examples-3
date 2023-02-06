package ru.yandex.msearch;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.LongList;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.CloseableDeleter;

public class BytesUsedLeakTest extends TestBase {
    private boolean hasShrinks(final LongList list, final String name) {
        int size = list.size();
        for (int i = 2; i < size; ++i) {
            long max = list.getLong(i);
            int shrinks = 0;
            for (int j = 0; j < i; ++j) {
                if (list.getLong(j) >= max) {
                    ++shrinks;
                    if (shrinks > 1) {
                        logger.info(
                            "At least two " + name
                            + " shrinks detected at batch\t#" + i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void testBytesUsedLeak(final int workers) throws Exception {
        final int docsCount = 25000;
        final int iterations = 320;
        final int statsBatches = 8;
        final int statsGranularity =
            (iterations + statsBatches - 1) / statsBatches;
        LongList mins = new LongList(statsBatches);
        LongList tops = new LongList(statsBatches);
        IndexWriterConfig config =
            new IndexWriterConfig(Version.LUCENE_40, new KeywordAnalyzer());
        try (CloseableDeleter tmpdir =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName()));
            IndexWriter writer =
                new IndexWriter(
                    FSDirectory.open(tmpdir.path().toFile()),
                    config,
                    Set.of("pk", "data"),
                    Collections.singleton("pk")))
        {
            ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                    workers,
                    workers,
                    1,
                    TimeUnit.DAYS,
                    new ArrayBlockingQueue<>(docsCount),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            executor.prestartAllCoreThreads();
            List<Future<?>> futures = new ArrayList<>(docsCount);
            StringBuilder sb = new StringBuilder(128);
            for (int i = 0; i < iterations; ++i) {
                logger.info("Iteration #" + i);
                futures.clear();
                for (int j = 0; j < docsCount; ++j) {
                    sb.setLength(0);
                    sb.append("some_long_prefix_here/");
                    sb.append(i);
                    sb.append("_with_insertions_");
                    sb.append(j);
                    futures.add(
                        executor.submit(new Task(writer, sb.toString())));
                }
                for (int j = 0; j < docsCount; ++j) {
                    futures.get(i).get();
                }
                writer.commit();
                writer.waitForMerges();
                long usage = writer.docWriterRawBytesUsed();
                logger.info("ram bytes size = " + usage);
                YandexAssert.assertLess(15000000L, usage);
                YandexAssert.assertGreater(0L, usage);

                int batchNo = i / statsGranularity;
                int pos = tops.size() - 1;
                if (batchNo > pos) {
                    tops.addLong(usage);
                    mins.addLong(usage);
                } else {
                    if (usage > tops.getLong(pos)) {
                        tops.setLong(pos, usage);
                    }
                    if (usage < mins.getLong(pos)) {
                        mins.setLong(pos, usage);
                    }
                }
            }
        } finally {
            logger.info("Usage dynamics:");
            int size = tops.size();
            for (int i = 0; i < size; ++i) {
                logger.info(
                    "Batch\t#" + i
                    + "\tmax usage: "
                    + tops.getLong(i)
                    + "\tmin usage: " + mins.getLong(i));
            }
        }
        if (hasShrinks(tops, "max") || hasShrinks(mins, "min")) {
            return;
        }
        Assert.fail("DocumentsWriter.bytesUsage leak detected");
    }

    @Test
    public void testBytesUsedLeak1() throws Exception {
        testBytesUsedLeak(IndexWriterConfig.DEFAULT_MAX_THREAD_STATES >> 1);
    }

    @Test
    public void testBytesUsedLeak2() throws Exception {
        testBytesUsedLeak(IndexWriterConfig.DEFAULT_MAX_THREAD_STATES << 1);
    }

    @Test
    public void testBytesUsedLeakViaSort() throws Exception {
        final int iterations = 80;
        final int statsBatches = 8;
        final int statsGranularity =
            (iterations + statsBatches - 1) / statsBatches;
        LongList mins = new LongList(statsBatches);
        LongList tops = new LongList(statsBatches);
        IndexWriterConfig config =
            new IndexWriterConfig(Version.LUCENE_40, new WhitespaceAnalyzer());
        try (CloseableDeleter tmpdir =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName()));
            IndexWriter writer =
                new IndexWriter(
                    FSDirectory.open(tmpdir.path().toFile()),
                    config,
                    Collections.singleton("field"),
                    Collections.singleton("field")))
        {
            final int tokensCount = BytesRefHash.UNSORTED_THRESHOLD * 2;
            StringBuilder sb = new StringBuilder(1024);
            for (int i = 0; i < iterations; ++i) {
                logger.info("Iteration #" + i);
                for (int j = 0; j < 10; ++j) {
                    sb.setLength(0);
                    for (int k = 0; k < tokensCount; ++k) {
                        sb.append(' ');
                        sb.append(i);
                        sb.append('_');
                        sb.append(j);
                        sb.append('_');
                        sb.append(k);
                    }
                    Document doc = new Document();
                    doc.add(
                        new Field(
                            "field",
                            sb.toString(),
                            Field.Store.YES,
                            Field.Index.ANALYZED,
                            Field.TermVector.NO));
                    writer.addDocument(doc);
                }
                writer.commit();
                writer.waitForMerges();
                long usage = writer.docWriterRawBytesUsed();
                logger.info("ram bytes size = " + usage);
                YandexAssert.assertLess(5000000L, usage);
                YandexAssert.assertGreater(0L, usage);

                int batchNo = i / statsGranularity;
                int pos = tops.size() - 1;
                if (batchNo > pos) {
                    tops.addLong(usage);
                    mins.addLong(usage);
                } else {
                    if (usage > tops.getLong(pos)) {
                        tops.setLong(pos, usage);
                    }
                    if (usage < mins.getLong(pos)) {
                        mins.setLong(pos, usage);
                    }
                }
            }
        } finally {
            logger.info("Usage dynamics:");
            int size = tops.size();
            for (int i = 0; i < size; ++i) {
                logger.info(
                    "Batch\t#" + i
                    + "\tmax usage: "
                    + tops.getLong(i)
                    + "\tmin usage: " + mins.getLong(i));
            }
        }
        if (hasShrinks(tops, "max") || hasShrinks(mins, "min")) {
            return;
        }
        Assert.fail("DocumentsWriter.bytesUsage leak detected");
    }

    private static class Task implements Runnable {
        private static final byte[] DATA = new byte[1536];

        private final IndexWriter writer;
        private final String pk;

        Task(final IndexWriter writer, final String pk) {
            this.writer = writer;
            this.pk = pk;
        }

        @Override
        public void run() {
            try {
                Document doc = new Document();
                doc.add(
                    new Field(
                        "pk",
                        pk,
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED_NO_NORMS,
                        Field.TermVector.NO));
                doc.add(new Field("data", DATA));
                writer.addDocument(doc);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

