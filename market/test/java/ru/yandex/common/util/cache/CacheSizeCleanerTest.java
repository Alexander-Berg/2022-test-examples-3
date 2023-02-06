package ru.yandex.common.util.cache;

import junit.framework.TestCase;

import java.util.Map;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class CacheSizeCleanerTest extends TestCase {
    private static final int COUNT = 1000;

    public void testCleaner() throws Exception {
        SimpleCache<String, String> simpleCache = new SimpleCache<>(new MockObjectProvider());
        simpleCache.setActualTimeInSeconds(20);
        simpleCache.setCacheSize(COUNT);
        for (int i = 0; i < COUNT; i++) {
            simpleCache.getCachedData("test" + i);
        }
        final Map<String, CachedDataWrapper<String>> values = simpleCache.getValues();
        assertEquals(COUNT, values.size());
        simpleCache.getCachedData("test" + COUNT);
        assertEquals(1, values.size());
    }

}
