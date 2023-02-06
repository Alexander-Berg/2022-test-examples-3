package ru.yandex.common.util.cache;

import junit.framework.TestCase;

import java.util.Map;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class SimpleCacheTest extends TestCase {
    private static final int COUNT = 1000;

    public void testCleaner() throws Exception {
        SimpleCache<String, String> simpleCache = new SimpleCache<>(new MockObjectProvider());

        simpleCache.setCleanerStartTime(50);
        simpleCache.setActualTime(50);
        for (int i = 0; i < COUNT; i++) {
            simpleCache.getCachedData("test" + i);
        }
        final Map<String, CachedDataWrapper<String>> values = simpleCache.getValues();
        Thread.sleep(300);
        assertEquals(0, values.size());
    }

}
