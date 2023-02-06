package ru.yandex.common.util;

import junit.framework.TestCase;
import org.junit.Test;

import ru.yandex.common.util.EnumUtilTest.EnumWithIntId;
import ru.yandex.common.util.EnumUtilTest.EnumWithStrId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EnumCachingUtilTest {
    @Test
    public void getById() {
        // when
        EnumWithIntId byIntId = EnumCachingUtil.getById(EnumWithIntId.class, 100);
        EnumWithStrId byStrId = EnumCachingUtil.getById(EnumWithStrId.class, "xxx");
        EnumWithIntId byIntIdMissing = EnumCachingUtil.getById(EnumWithIntId.class, 100500);
        EnumWithStrId byStrIdMissing = EnumCachingUtil.getById(EnumWithStrId.class, "missing");
        EnumWithIntId byIntIdNull = EnumCachingUtil.getById(EnumWithIntId.class, null);
        EnumWithStrId byStrIdNull = EnumCachingUtil.getById(EnumWithStrId.class, null);

        // then
        assertEquals(EnumWithIntId.VALUE, byIntId);
        assertEquals(EnumWithStrId.VALUE, byStrId);
        assertNull(byIntIdMissing);
        assertNull(byIntIdNull);
        assertNull(byStrIdMissing);
        assertNull(byStrIdNull);
    }
}
