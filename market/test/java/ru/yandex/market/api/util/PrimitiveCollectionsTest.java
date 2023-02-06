package ru.yandex.market.api.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Test;

import java.util.Arrays;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 10.11.16.
 */
public class PrimitiveCollectionsTest extends UnitTestBase {

    @Test
    public void testDistinctLongList() {
        LongList list = new LongArrayList(new long[] {1, 2, 3, 4, 5, 3, 7});
        list = PrimitiveCollections.distinct(list);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 7L), list);
    }

    @Test
    public void testDistinctWithNullList() {
        assertNull(PrimitiveCollections.distinct(null));
    }
}
