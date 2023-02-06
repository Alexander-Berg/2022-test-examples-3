package ru.yandex.market.crm.platform.common;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UidValueType;

@RunWith(Parameterized.class)
public class UidTypesElementsTest {

    @Parameterized.Parameter
    public UidType type;

    @Parameterized.Parameters(name = "{index}: type {0}")
    public static Iterable<Object[]> data() {
        List<UidType> types = Arrays.asList(UidType.values());
        Iterable<UidType> withoutUnrecognized = Iterables.filter(types, t -> !UidType.UNRECOGNIZED.equals(t));
        return Iterables.transform(withoutUnrecognized, t -> new Object[]{t});
    }

    @Test
    public void cryptaWithoutError() {
        // проверяем отсутствие ошибки при вызове метода. Все возможные значения, возвращаемые методом, валидны
        UidTypes.crypta(type);
    }

    @Test
    public void offlineWithoutError() {
        // проверяем отсутствие ошибки при вызове метода. Все возможные значения, возвращаемые методом, валидны
        UidTypes.offline(type);
    }

    @Test
    public void typeNotNull() {
        UidValueType result = UidTypes.type(type);
        Assert.assertNotNull("Должны получить тип значения", result);
    }

    @Test
    public void valueNotNull() {
        String result = UidTypes.value(type);
        Assert.assertNotNull("Должны получить value", result);
    }
}
