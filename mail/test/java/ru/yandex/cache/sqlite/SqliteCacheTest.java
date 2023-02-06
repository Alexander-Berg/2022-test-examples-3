package ru.yandex.cache.sqlite;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.cache.CacheEntry;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.unsafe.NativeMemory2;
import ru.yandex.util.filesystem.DeletingFileVisitor;

public class SqliteCacheTest extends TestBase {
    private static final long TEST_DB_SIZE = 10000000L;
    private static final int TEST_BLOCK_SIZE = 3333;
    private static final int ASYNC_SLEEP = 1000;
    private static final int K = 1000;
    private static final int HK = 500;
    private static final NativeMemory2.NativeMemoryAllocator ALLOCATOR =
        NativeMemory2.NativeMemoryAllocator.get("sqlite-test");

    @Test
    public void testSizeLimit() throws Exception {
        File root = Files.createTempDirectory("test").toFile();
        try {
            File dbFile = new File(root, "test.db");
            logger.info("Opening cache at: " + root.getCanonicalPath());
            SqliteCache cache =
                new SqliteCache(
                    dbFile.getCanonicalPath(),
                    TEST_DB_SIZE,
                    1L);
            NativeMemory2 buf = ALLOCATOR.alloc(TEST_BLOCK_SIZE << 1);
            buf.fill((byte) 0x2);
            int testCount = (int) (TEST_DB_SIZE / TEST_BLOCK_SIZE);
            Random r = new Random();
            for (int i = 0; i < testCount; i++) {
                int randDiff = r.nextInt(K) - HK;
                cache.put(
                    "/test/dir/test/file:" + i,
                    buf.address(),
                    TEST_BLOCK_SIZE + randDiff,
                    (TEST_BLOCK_SIZE + randDiff) << 1,
                    1,
                    false);
            }
            cache.flush();
            Thread.sleep(ASYNC_SLEEP);
            YandexAssert.assertLess(TEST_DB_SIZE, dbFile.length());
            //test remove prefix speed
            long start = System.currentTimeMillis();
            cache.removePrefix("/test/dir/test/file");
            long time = System.currentTimeMillis() - start;
            logger.info("Prefix removed in " + time + " ms");
            cache.close();
        } finally {
            removeDirectory(root);
        }
    }

    @Test
    public void testIntegrity() throws Exception {
        File root = Files.createTempDirectory("test1").toFile();
        try {
            File dbFile = new File(root, "test1.db");
            logger.info("Opening cache at:  " + root.getCanonicalPath());
            SqliteCache cache =
                new SqliteCache(
                    dbFile.getCanonicalPath(),
                    TEST_DB_SIZE);
            NativeMemory2 buf = ALLOCATOR.alloc(TEST_BLOCK_SIZE);
            buf.fill((byte) 0x2);
            String key = "testKey";
            cache.put(key, buf.address(), buf.size(), buf.size() << 1, 1, true);
            CacheEntry entry = cache.get(key, true);
            Assert.assertEquals(buf.size(), entry.compressedSize());
            Assert.assertEquals(buf.size() << 1, entry.decompressedSize());
            for (int i = 0; i < buf.size(); i++) {
                byte real = buf.getByte(i);
                byte test = NativeMemory2.unboxedGetByte(entry.address() + i);
                if (real != test) {
                    logger.info("Bytes missmatched at " + i
                        + ", real=" + Integer.toHexString(real)
                        + ", test=" + Integer.toHexString(test));
                    Assert.assertTrue(false);
                }
            }
            cache.remove(key);
            entry = cache.get(key, true);
            Assert.assertEquals(null, entry);

            //test async put
            cache.put(
                key,
                buf.address(),
                buf.size(),
                buf.size() << 1,
                1,
                false);
            Thread.sleep(ASYNC_SLEEP);
            entry = cache.get(key, true);
            Assert.assertEquals(buf.size(), entry.compressedSize());
            Assert.assertEquals(buf.size() << 1, entry.decompressedSize());
            for (int i = 0; i < buf.size(); i++) {
                byte real = buf.getByte(i);
                byte test = NativeMemory2.unboxedGetByte(entry.address() + i);
                if (real != test) {
                    logger.info("Bytes missmatched at  " + i
                        + ", real = " + Integer.toHexString(real)
                        + ", test = " + Integer.toHexString(test));
                    Assert.assertTrue(false);
                }
            }
            cache.close();
        } finally {
            removeDirectory(root);
        }
    }

    @Test
    public void testUniqFiles() throws Exception {
        File root = Files.createTempDirectory("test2").toFile();
        try {
            File dbFile = new File(root, "test2.db");
            logger.info("Opening cache at : " + root.getCanonicalPath());
            SqliteCache cache =
                new SqliteCache(
                    dbFile.getCanonicalPath(),
                    TEST_DB_SIZE,
                    1L);
            NativeMemory2 buf = ALLOCATOR.alloc(TEST_BLOCK_SIZE);
            buf.fill((byte) 0x2);
            String[] keys = new String[] {
                "/test/dir/test/file1",
                "/test/dir/test/file2",
                "/test/dir/test/file3"
            };
            for (String key: keys) {
                cache.put(
                    key + ":124",
                    buf.address(),
                    buf.size(),
                    buf.size(),
                    1,
                    true);
            }
            String[] files = cache.uniqFiles();
            Assert.assertTrue(
                new HashSet<String>(Arrays.asList(keys)).equals(
                    new HashSet<String>(Arrays.asList(files))));
            cache.close();
        } finally {
            removeDirectory(root);
        }
    }

    @Test
    public void testUniqFilesWithSymbols() throws Exception {
        File root = Files.createTempDirectory("test3").toFile();
        try {
            File dbFile = new File(root, "test3.db");
            logger.info(
                "Opening cache3 at : " + root.getCanonicalPath());
            SqliteCache cache =
                new SqliteCache(
                    dbFile.getCanonicalPath(),
                    TEST_DB_SIZE,
                    1L);
            NativeMemory2 buf = ALLOCATOR.alloc(TEST_BLOCK_SIZE);
            buf.fill((byte) 0x2);
            String[] keys = new String[] {
                "/test/dir/test/f#ile2",
                "/test/dir/test/f$ile3",
                "/test/dir/test/f:ile1"
            };
            for (String key: keys) {
                cache.put(
                    key + ":123",
                    buf.address(),
                    buf.size(),
                    buf.size(),
                    1,
                    true);
            }
            String[] files = cache.uniqFiles();
            Arrays.sort(files);
            Assert.assertEquals(
                Arrays.toString(keys),
                Arrays.toString(files));
            cache.close();
        } finally {
            removeDirectory(root);
        }
    }

    public static void removeDirectory(final File directory) throws Exception {
        Files.walkFileTree(directory.toPath(), DeletingFileVisitor.INSTANCE);
    }
}
