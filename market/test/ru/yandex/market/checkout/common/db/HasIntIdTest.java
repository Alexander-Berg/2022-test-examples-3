package ru.yandex.market.checkout.common.db;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HasIntIdTest {

    @Test
    public void valueById() {
//        assertEquals(Color.UNKNOWN, HasIntId.valueById(null, Color.values(), Color.UNKNOWN));
        assertEquals(Color.UNKNOWN, HasIntId.valueById(-1, Color.values(), Color.UNKNOWN));
        assertEquals(Color.BLUE, HasIntId.valueById(1, Color.values(), Color.UNKNOWN));
    }
}
