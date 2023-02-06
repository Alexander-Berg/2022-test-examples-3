package ru.yandex.crypta.graph;

import java.util.Random;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class YandexuidTest {
    @Test
    public void testIsValid() {
        Yandexuid test1 = new Yandexuid("");
        Yandexuid test2 = new Yandexuid("0");
        Yandexuid test3 = new Yandexuid("-1");
        Yandexuid test4 = new Yandexuid("10113701529442803");
        Yandexuid test5 = new Yandexuid("0011188541530035229");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "0");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "-1");
        assertEquals(test3.isValid(), false);
        assertEquals(test4.getValue(), "10113701529442803");
        assertEquals(test4.isValid(), true);
        assertEquals(test5.getValue(), "0011188541530035229");
        assertEquals(test5.isValid(), true);
    }

    @Test
    public void testGetType() {
        Yandexuid test = new Yandexuid("");
        assertEquals(test.getType(), EIdType.YANDEXUID);
    }

    @Test
    public void testEquals() {
        var value = Long.toString(new Random().nextLong());
        var left = new Yandexuid(value);
        var right = new Yandexuid(value);
        var other = new Yandexuid("");
        assertEquals(left, right);
        assertNotEquals(left, other);
        assertNotEquals(right, other);
    }

    @Test
    public void testHashCode() {
        var value = Long.toString(new Random().nextLong());
        var left = new Yandexuid(value);
        var right = new Yandexuid(value);
        assertEquals(left.hashCode(), right.hashCode());
    }
}
