package ru.yandex.market.pricelabs.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HideChangeTest {

    @Test
    void testNull() {
        Assertions.assertThrows(NullPointerException.class, () -> new HideChange(null, 1));
    }

}
