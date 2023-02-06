package ru.yandex.market.crm.platform.common;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UidValueType;

public class UidTypesTest {

    @Test
    public void crypta_false() {
        boolean result = UidTypes.crypta(UidType.KINOPOISK_ID);
        Assert.assertFalse("Должны получить false т.к. option crypta в proto-файле не установлено", result);
    }

    @Test
    public void crypta_true() {
        boolean result = UidTypes.crypta(UidType.PUID);
        Assert.assertTrue("Должны получить true т.к. option crypta в proto-файле установлено в true", result);
    }

    @Test
    public void offline_false() {
        boolean result = UidTypes.offline(UidType.KINOPOISK_ID);
        Assert.assertFalse("Должны получить false т.к. option offline в proto-файле не установлено", result);
    }

    @Test
    public void offline_true() {
        boolean result = UidTypes.offline(UidType.SBER_ID);
        Assert.assertTrue("Должны получить true т.к. option offline в proto-файле установлено в true", result);
    }

    @Test
    public void value() {
        String result = UidTypes.value(UidType.KINOPOISK_ID);
        Assert.assertEquals("Должны получить значение из proto-файла", "kp_id", result);
    }

    @Test
    public void valueType_int() {
        UidValueType result = UidTypes.type(UidType.PUID);
        Assert.assertEquals("Должны получить значение из proto-файла", UidValueType.INT, result);
    }

    @Test
    public void valueType_string() {
        UidValueType result = UidTypes.type(UidType.YANDEXUID);
        Assert.assertEquals("Должны получить значение из proto-файла", UidValueType.STRING, result);
    }

    @Test
    public void valueOf_exists_byName() {
        UidType result = UidTypes.valueOf("KINOPOISK_id");
        Assert.assertEquals("Должны получить по имени enum-a", UidType.KINOPOISK_ID, result);
    }

    @Test
    public void valueOf_exists_byValue() {
        UidType result = UidTypes.valueOf("kp_ID");
        Assert.assertEquals("Должны получить по value enum-a", UidType.KINOPOISK_ID, result);
    }

    @Test
    public void valueOf_notExists() {
        UidType result = UidTypes.valueOf("value_not_exist");
        Assert.assertEquals("Должны получить UNRECOGNIZED т.к. нет элемента со значением 'value_not_exist'",
                UidType.UNRECOGNIZED, result);
    }

    @Test(expected = NullPointerException.class)
    public void valueOf_null() {
        UidTypes.valueOf(null);
    }
}
