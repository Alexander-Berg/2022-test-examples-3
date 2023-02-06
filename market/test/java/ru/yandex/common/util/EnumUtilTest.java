package ru.yandex.common.util;

import org.junit.Test;

import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.id.HasId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Date: 12/1/11
 *
 * @author btv (btv@yandex-team.ru)
 */
public class EnumUtilTest {
    enum TestEnum {
        VALUE1,
        VALUE2,
        VALUE3,
        ;
    }

    enum EnumWithIntId implements HasId<Integer> {
        VALUE {
            @Override
            public Integer getId() {
                return 100;
            }
        }

    }
    enum EnumWithStrId implements HasId<String> {
        VALUE {
            @Override
            public String getId() {
                return "xxx";
            }
        }
    }

    @Test
    public void testValueExtractor() {
        assertEquals(Cf.list(TestEnum.values()), EnumUtil.valueExtractor(TestEnum.class).map(Cf.list("VALUE1", "VALUE2", "VALUE3")));
        assertEquals(Cf.list(TestEnum.VALUE2), EnumUtil.valueExtractor(TestEnum.class).map(Cf.list("VALUE2")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException() {
        EnumUtil.valueExtractor(TestEnum.class).apply("VALUE_");
    }

    @Test
    public void getById() {
        // when
        EnumWithIntId byIntId = EnumUtil.getById(EnumWithIntId.class, 100);
        EnumWithStrId byStrId = EnumUtil.getById(EnumWithStrId.class, "xxx");
        EnumWithIntId byIntIdMissing = EnumUtil.getById(EnumWithIntId.class, 100500);
        EnumWithStrId byStrIdMissing = EnumUtil.getById(EnumWithStrId.class, "missing");
        EnumWithIntId byIntIdNull = EnumUtil.getById(EnumWithIntId.class, null);
        EnumWithStrId byStrIdNull = EnumUtil.getById(EnumWithStrId.class, null);

        // then
        assertEquals(EnumWithIntId.VALUE, byIntId);
        assertEquals(EnumWithStrId.VALUE, byStrId);
        assertNull(byIntIdMissing);
        assertNull(byIntIdNull);
        assertNull(byStrIdMissing);
        assertNull(byStrIdNull);
    }
}
