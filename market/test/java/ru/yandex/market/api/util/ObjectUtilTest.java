package ru.yandex.market.api.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ObjectUtilTest {

    @Test
    public void shouldFindNullForAnyIsNull() throws Exception {
        assertTrue(ObjectUtil.anyIsNull(Integer.valueOf(1), null));
        assertFalse(ObjectUtil.anyIsNull(Integer.valueOf(1), Integer.valueOf(2)));
    }

    @Test
    public void shouldFindNonNullForAnyNonNull() throws Exception {
        assertTrue(ObjectUtil.anyNonNull(null, Integer.valueOf(1)));
        assertFalse(ObjectUtil.anyNonNull(null, null));
    }

    @Test
    public void shouldFindNullForAllNonNull() throws Exception {
        assertTrue(ObjectUtil.allNonNull(Integer.valueOf(1), Integer.valueOf(2)));
        assertFalse(ObjectUtil.allNonNull(Integer.valueOf(1), null));
    }

    @Test
    public void shouldFindNonNullForAllIsNull() throws Exception {
        assertTrue(ObjectUtil.allIsNull(null, null));
        assertFalse(ObjectUtil.allIsNull(null, Integer.valueOf(1)));
    }
}
