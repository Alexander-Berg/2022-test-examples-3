package ru.yandex.common.util.db;

import junit.framework.TestCase;
import ru.yandex.common.util.collections.Cf;

import java.util.Date;
import java.util.Map;

/**
 * @author leftie
 *         <p/>
 *         Created 23.11.12 15:51
 */
public class RowCallbacksTest extends TestCase {

    public void testRowMappers() throws Exception {
        final Map<Long, Date> tmp = Cf.newUnorderedMap();
        RowCallbacks.addToMap(tmp, RowMappers.longAt(1), RowMappers.timestampAt(2));
    }
}
