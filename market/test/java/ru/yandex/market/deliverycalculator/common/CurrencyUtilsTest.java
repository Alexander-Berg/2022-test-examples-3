package ru.yandex.market.deliverycalculator.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест для {@link CurrencyUtils}
 */
class CurrencyUtilsTest {

    /**
     * Тест для {@link CurrencyUtils#convertToPenny(double)}.
     */
    @Test
    void testConvertToPenny() {
        assertEquals(156789L, CurrencyUtils.convertToPenny(1567.89));
        assertEquals(156789L, CurrencyUtils.convertToPenny(1567.8912));
        assertEquals(156789L, CurrencyUtils.convertToPenny(1567.8992));
        assertEquals(5L, CurrencyUtils.convertToPenny(0.05));
        assertEquals(0L, CurrencyUtils.convertToPenny(0.005));
        assertEquals(100L, CurrencyUtils.convertToPenny(1));
    }
}
