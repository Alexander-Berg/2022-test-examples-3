package ru.yandex.market.crm.platform.common;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.util.Randoms;

public class UidsTest {

    @Test
    public void create_int_int() {
        long value = Randoms.longValue();

        Uid result = Uids.create(UidType.PUID, value);

        Assert.assertEquals(UidType.PUID, result.getType());
        Assert.assertEquals(value, result.getIntValue());
    }

    @Test
    public void create_int_string() {
        long value = Randoms.longValue();

        Uid result = Uids.create(UidType.PUID, String.valueOf(value));

        Assert.assertEquals(UidType.PUID, result.getType());
        Assert.assertEquals(value, result.getIntValue());
    }

    @Test
    public void create_string_int() {
        long value = Randoms.longValue();

        Uid result = Uids.create(UidType.YANDEXUID, value);

        Assert.assertEquals(UidType.YANDEXUID, result.getType());
        Assert.assertEquals(String.valueOf(value), result.getStringValue());
    }

    @Test
    public void create_string_string() {
        String value = Randoms.string();

        Uid result = Uids.create(UidType.YANDEXUID, value);

        Assert.assertEquals(UidType.YANDEXUID, result.getType());
        Assert.assertEquals(value, result.getStringValue());
    }

    @Test
    public void asStringValue_int() {
        long value = Randoms.longValue();

        Uid uid = Uid.newBuilder()
                .setIntValue(value)
                .build();

        String result = Uids.asStringValue(uid);

        Assert.assertEquals(String.valueOf(value), result);
    }

    @Test
    public void asStringValue_null() {
        Uid uid = Uid.newBuilder()
                .build();

        String result = Uids.asStringValue(uid);

        Assert.assertNull(result);
    }

    @Test
    public void asStringValue_string() {
        String value = Randoms.string();

        Uid uid = Uid.newBuilder()
                .setStringValue(value)
                .build();

        String result = Uids.asStringValue(uid);

        Assert.assertEquals(value, result);
    }

    @Test
    public void getPriorityUid() {
        Collection<Uid> uids = Arrays.asList(
                Uids.create(UidType.YANDEXUID, "1"),
                Uids.create(UidType.PUID, 1)
        );
        Uid result = Uids.getPriorityUid(uids);
        Assert.assertEquals(UidType.PUID, result.getType());
    }
}
