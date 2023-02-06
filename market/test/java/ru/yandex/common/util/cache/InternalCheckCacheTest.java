/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 16.09.2006</p>
 * <p>Time: 14:47:47</p>
 */
package ru.yandex.common.util.cache;

import junit.framework.TestCase;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class InternalCheckCacheTest extends TestCase {
    public void testGetByCheck() throws Exception {
        final MockObjectProvider objectProvider = new MockObjectProvider();
        InternalCheckCache<String, String> cache = new InternalCheckCache<String, String>(objectProvider);
        final String cachedData = cache.getCachedData("test", "v1");
        final String cachedData2 = cache.getCachedData("test", "v1");
        assertEquals(1, objectProvider.getCallCounter());
        assertSame(cachedData, cachedData2);
        cache.getCachedData("test", "v2");
        assertEquals(2, objectProvider.getCallCounter());
        cache.getCachedData("test", "v2");
        assertEquals(2, objectProvider.getCallCounter());
        cache.getCachedData("test", "v1");
        assertEquals(3, objectProvider.getCallCounter());
    }
}
