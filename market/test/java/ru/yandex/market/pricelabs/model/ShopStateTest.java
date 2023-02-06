package ru.yandex.market.pricelabs.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopStateTest {

    @Test
    void testSchedule() {
        var state = new ShopState(1, List.of(), "", "{\"shop:loop\":{\"frequency\":240}}");
        assertEquals(240, state.getShopLoopFreqMinutes());
    }

}
