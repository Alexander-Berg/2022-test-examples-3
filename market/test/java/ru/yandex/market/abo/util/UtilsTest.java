package ru.yandex.market.abo.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 16/11/2020.
 */
class UtilsTest {

    @Test
    void filterNew() {
        assertEquals(List.of(1), Utils.filterNew(List.of(1, 2, 3), ids -> List.of(2, 3, 4)));
    }
}