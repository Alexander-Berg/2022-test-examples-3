package ru.yandex.market.pers.notify.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author semin-serg
 */
public class CurrencyUtilTest {

    private static final String NO_BREAK_SPACE = "\u00A0";

    @Test
    public void priceToStringFromBigDecimalTest1() {
        final long minNumberWithFourSigns = 1000L;
        priceToStringFromBigDecimalTest("1" + NO_BREAK_SPACE + "000", BigDecimal.valueOf(minNumberWithFourSigns));
    }

    @Test
    public void priceToStringFromBigDecimalTest2() {
        final long maxNumberWithThreeSigns = 999L;
        priceToStringFromBigDecimalTest("999", BigDecimal.valueOf(maxNumberWithThreeSigns));
    }

    @Test
    public void priceToStringFromBigDecimalTest3() {
        priceToStringFromBigDecimalTest(null, null);
    }

    @Test
    public void priceToStringFromStringTest1() {
        priceToStringFromStringTest("1" + NO_BREAK_SPACE + "000", "1000");
    }

    @Test
    public void priceToStringFromStringTest2() {
        priceToStringFromStringTest("999", "999");
    }

    @Test
    public void priceToStringFromStringTest3() {
        priceToStringFromStringTest(null, null);
    }

    @Test
    public void priceToStringFromDoubleTest1() {
        final double minNumberWithFourSigns = 1000.0;
        priceToStringFromDoubleTest("1" + NO_BREAK_SPACE + "000", minNumberWithFourSigns);
    }

    @Test
    public void priceToStringFromDoubleTest2() {
        final double maxNumberWithThreeSigns = 999.0;
        priceToStringFromDoubleTest("999", maxNumberWithThreeSigns);
    }

    @Test
    public void priceToStringFromDoubleTest3() {
        priceToStringFromDoubleTest(null, null);
    }

    private void priceToStringFromBigDecimalTest(String expectedResult, BigDecimal sourcePrice) {
        String actualResult = CurrencyUtil.priceToString(sourcePrice);
        assertEquals(expectedResult, actualResult);
    }

    private void priceToStringFromStringTest(String expectedResult, String sourcePrice) {
        String actualResult = CurrencyUtil.priceToString(sourcePrice);
        assertEquals(expectedResult, actualResult);
    }

    private void priceToStringFromDoubleTest(String expectedResult, Double sourcePrice) {
        String actualResult = CurrencyUtil.priceToString(sourcePrice);
        assertEquals(expectedResult, actualResult);
    }
}
