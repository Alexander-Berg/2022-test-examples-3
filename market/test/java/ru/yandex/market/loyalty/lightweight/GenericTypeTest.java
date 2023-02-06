package ru.yandex.market.loyalty.lightweight;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("AnonymousInnerClassMayBeStatic")
public class GenericTypeTest {
    @Test
    public void simpleType() {
        GenericType<Integer> genericType = new GenericType<Integer>() {
        };
        Class<Integer> type = genericType.getType();
        assertEquals(Integer.class, type);
    }

    @Test
    public void genericType() {
        GenericType<List<Integer>> genericType = new GenericType<List<Integer>>() {
        };
        Class<List<Integer>> type = genericType.getType();
        assertEquals(List.class, type);
    }
}
