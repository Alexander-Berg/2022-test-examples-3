package ru.yandex.market.crm.platform.common;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;

@RunWith(Parameterized.class)
public class UidsCompareTest {

    @Parameterized.Parameter(0)
    public Uid o1;
    @Parameterized.Parameter(1)
    public Uid o2;
    @Parameterized.Parameter(2)
    public int expected;

    @Parameterized.Parameters(name = "{index}: compare({0}, {1}) = {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Uids.create(UidType.EMAIL, "a@a.ru"), Uids.create(UidType.EMAIL, "a@a.ru"), 0},
                {Uids.create(UidType.EMAIL, "a@a.ru"), Uids.create(UidType.EMAIL, "b@a.ru"), -1},
                {Uids.create(UidType.EMAIL, "b@a.ru"), Uids.create(UidType.EMAIL, "a@a.ru"), 1},
                {null, null, 0},
                {null, Uids.create(UidType.EMAIL, "a@a.ru"), 1},
                {Uids.create(UidType.EMAIL, "a@a.ru"), null, -1},
                {Uids.create(UidType.EMAIL, "a@a.ru"), Uids.create(UidType.PUID, "1"), 1},
                {Uids.create(UidType.YANDEXUID, "1"), Uids.create(UidType.PUID, "1"), 1},
                {Uids.create(UidType.PUID, "1"), Uids.create(UidType.YANDEXUID, "1"), -1},
        });
    }

    @Test
    public void compare() {
        int result = Uids.compare(o1, o2);

        if (0 == expected) {
            Assert.assertTrue(0 == result);
        } else if (0 > expected) {
            Assert.assertTrue(0 > result);
        } else {
            Assert.assertTrue(0 < result);
        }
    }
}
