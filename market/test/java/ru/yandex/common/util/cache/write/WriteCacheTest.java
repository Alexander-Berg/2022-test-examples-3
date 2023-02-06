package ru.yandex.common.util.cache.write;

import junit.framework.TestCase;

import java.util.List;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class WriteCacheTest extends TestCase {
    private static final int COUNT = 10;

    public void testWriterCleaner() throws Exception {
        WriteAggregationCache<String> writeCache = getCache();

        writeCache.setActualTime(50);
        for (int i = 0; i < COUNT; i++) {
            writeCache.put("test" + i);
        }
        final List<String> values = writeCache.getValues();
        assertEquals(COUNT, values.size());
        Thread.sleep(300);
        assertEquals(0, values.size());
    }

    public void testNoCacheMode() throws Exception {
        WriteAggregationCache<String> writeCache = getCache();

        writeCache.setActualTime(0);
        for (int i = 0; i < COUNT; i++) {
            writeCache.put("test" + i);
        }
        final List<String> values = writeCache.getValues();
        assertEquals(0, values.size());
        Thread.sleep(300);
        assertEquals(0, values.size());
    }

    private WriteAggregationCache<String> getCache() {
        WriteAggregationCache<String> writeCache = new WriteAggregationCache<>();
        writeCache.setObjectsWriter(values -> System.out.println("Writing " + values.size() + " values"));
        return writeCache;
    }

}
