package ru.yandex.common.util.cache;

import junit.framework.TestCase;

import java.util.Map;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class ExpensiveCheckCacheTest extends TestCase {
    private static final int COUNT = 100;

    public void testCleaner() throws Exception {
        ExpensiveCheckCache<String, String> expensiveCheckCache =
                new ExpensiveCheckCache<String, String>(new MockObjectProvider(), new MockExpensiveCheckProvider());
        expensiveCheckCache.setActualTime(2*1000L);
        expensiveCheckCache.setCleanerStartTime(2000);
        expensiveCheckCache.setExpensiveCheckTime(100);
        for (int i = 0; i < COUNT; i++) {
            expensiveCheckCache.getCachedData("test" + i);
        }
        for (int i = 0; i < COUNT; i++) {
            expensiveCheckCache.getCachedData("test" + i);
        }
        final Map<String, CachedDataWrapper<String>> values = expensiveCheckCache.getValues();
        Thread.sleep(5*1000L);
        assertEquals(0, values.size());
    }

    private static class MockExpensiveCheckProvider implements ExpensiveCheckProvider {
        private int count;

        @Override
        public Object getObjectCheck() {
            count++;
            if (count > COUNT + COUNT/2) {
                return "myCheck";
            } else {
                return "yourCheck";
            }

        }
    }

}