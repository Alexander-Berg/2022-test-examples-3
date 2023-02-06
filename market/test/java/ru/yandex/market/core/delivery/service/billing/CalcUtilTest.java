package ru.yandex.market.core.delivery.service.billing;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест для {@link CalcUtil}.
 */
public class CalcUtilTest {

    @Test
    void toKilosDoublesTest() {
        assertEquals(0.0001, CalcUtil.toKilos(0.1));
        assertEquals(0.00001, CalcUtil.toKilos(0.01));
        assertEquals(0.001, CalcUtil.toKilos(1.));
        assertEquals(0.031, CalcUtil.toKilos(31.));
        assertEquals(0.513, CalcUtil.toKilos(513.));
        assertEquals(1.024, CalcUtil.toKilos(1024.));
    }

    @Test
    void toKilosBigDecimalTest() {
        assertEquals(BigDecimal.valueOf(0), CalcUtil.toKilos(Long.valueOf(0L)));
        assertEquals(BigDecimal.valueOf(0.001), CalcUtil.toKilos(Long.valueOf(1L)));
        assertEquals(BigDecimal.valueOf(0.031), CalcUtil.toKilos(Long.valueOf(31L)));
        assertEquals(BigDecimal.valueOf(0.513), CalcUtil.toKilos(Long.valueOf(513L)));
        assertEquals(BigDecimal.valueOf(1.024), CalcUtil.toKilos(Long.valueOf(1024L)));
    }

    @Test
    void toGramsDoubleTest() {
        assertEquals(0.1, CalcUtil.toGrams(0.0001));
        assertEquals(1, CalcUtil.toGrams(0.001));
        assertEquals(31, CalcUtil.toGrams(0.031));
        assertEquals(513, CalcUtil.toGrams(0.513));
        assertEquals(1024, CalcUtil.toGrams(1.024));
    }

    @Test
    void toGramsBigDecimalTest() {
        assertEquals(1L, CalcUtil.toGrams(BigDecimal.valueOf(0.001)));
        assertEquals(10L, CalcUtil.toGrams(BigDecimal.valueOf(0.01)));
        assertEquals(100L, CalcUtil.toGrams(BigDecimal.valueOf(0.1)));
        assertEquals(1000L, CalcUtil.toGrams(BigDecimal.valueOf(1L)));
        assertEquals(31000L, CalcUtil.toGrams(BigDecimal.valueOf(31L)));
        assertEquals(513000L, CalcUtil.toGrams(BigDecimal.valueOf(513L)));
        assertEquals(1024000L, CalcUtil.toGrams(BigDecimal.valueOf(1024L)));
    }

    @Test
    void fromKilosToMillisTest() {
        assertEquals(1000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(0.001)));
        assertEquals(10000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(0.01)));
        assertEquals(100000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(0.1)));
        assertEquals(1000000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(1L)));
        assertEquals(31000000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(31L)));
        assertEquals(513000000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(513L)));
        assertEquals(1024000000L, CalcUtil.fromKilosToMillis(BigDecimal.valueOf(1024L)));
    }

    @Test
    void fromMillisToKilosTest() {
        assertEquals(BigDecimal.valueOf(0), CalcUtil.fromMillisToKilos(0L));
        assertEquals(BigDecimal.valueOf(0.000031), CalcUtil.fromMillisToKilos(31L));
        assertEquals(BigDecimal.valueOf(0.000513), CalcUtil.fromMillisToKilos(513L));
        assertEquals(BigDecimal.valueOf(0.001024), CalcUtil.fromMillisToKilos(1024L));
        assertEquals(BigDecimal.valueOf(0.2048), CalcUtil.fromMillisToKilos(204800L));
    }

    @Test
    void fromCentiToMicroTest() {
        assertEquals(10L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(0.001)));
        assertEquals(100L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(0.01)));
        assertEquals(1000L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(0.1)));
        assertEquals(10000L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(1L)));
        assertEquals(310000L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(31L)));
        assertEquals(5130000L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(513L)));
        assertEquals(10240000L, CalcUtil.fromCentiToMicro(BigDecimal.valueOf(1024L)));
    }

    @Test
    void fromMicroToCentiTest() {
        assertEquals(BigDecimal.valueOf(0), CalcUtil.fromMicroToCenti(0L));
        assertEquals(BigDecimal.valueOf(0.0031), CalcUtil.fromMicroToCenti(31L));
        assertEquals(BigDecimal.valueOf(0.0513), CalcUtil.fromMicroToCenti(513L));
        assertEquals(BigDecimal.valueOf(0.1024), CalcUtil.fromMicroToCenti(1024L));
        assertEquals(BigDecimal.valueOf(20.48), CalcUtil.fromMicroToCenti(204800L));
    }

    @Test
    void microToKilosTest() {
        assertEquals(BigDecimal.valueOf(0.205), CalcUtil.microToKilos(BigDecimal.valueOf(204800L)));
        assertEquals(BigDecimal.valueOf(1.234), CalcUtil.microToKilos(BigDecimal.valueOf(1234000L)));
    }

    @Test
    void toKopeckTest() {
        assertEquals(BigDecimal.valueOf(0), CalcUtil.toKopeck(BigDecimal.valueOf(0L)));
        assertEquals(BigDecimal.valueOf(100), CalcUtil.toKopeck(BigDecimal.valueOf(1L)));
        assertEquals(BigDecimal.valueOf(31100), CalcUtil.toKopeck(BigDecimal.valueOf(311L)));
        assertEquals(BigDecimal.valueOf(513000), CalcUtil.toKopeck(BigDecimal.valueOf(5130L)));
        assertEquals(BigDecimal.valueOf(12340000), CalcUtil.toKopeck(BigDecimal.valueOf(123400L)));
    }

    @Test
    void toKopeckLongTest() {
        assertEquals(0, CalcUtil.toKopeckLong(BigDecimal.valueOf(0L)));
        assertEquals(100, CalcUtil.toKopeckLong(BigDecimal.valueOf(1L)));
        assertEquals(31100, CalcUtil.toKopeckLong(BigDecimal.valueOf(311L)));
        assertEquals(513000, CalcUtil.toKopeckLong(BigDecimal.valueOf(5130L)));
        assertEquals(12340000, CalcUtil.toKopeckLong(BigDecimal.valueOf(123400L)));
    }

    @Test
    void toRoublesTest() {
        assertEquals(BigDecimal.valueOf(0).movePointLeft(2), CalcUtil.toRoubles(BigDecimal.valueOf(0L)));
        assertEquals(BigDecimal.valueOf(1).movePointLeft(2), CalcUtil.toRoubles(BigDecimal.valueOf(1L)));
        assertEquals(BigDecimal.valueOf(311).movePointLeft(2), CalcUtil.toRoubles(BigDecimal.valueOf(311L)));
        assertEquals(BigDecimal.valueOf(5130).movePointLeft(2), CalcUtil.toRoubles(BigDecimal.valueOf(5130L)));
        assertEquals(BigDecimal.valueOf(123400).movePointLeft(2), CalcUtil.toRoubles(BigDecimal.valueOf(123400L)));
    }

    @Test
    void toRoublesLongTest() {
        assertEquals(BigDecimal.valueOf(0).movePointLeft(2), CalcUtil.toRoubles(0L));
        assertEquals(BigDecimal.valueOf(1).movePointLeft(2), CalcUtil.toRoubles(1L));
        assertEquals(BigDecimal.valueOf(311).movePointLeft(2), CalcUtil.toRoubles(311L));
        assertEquals(BigDecimal.valueOf(5130).movePointLeft(2), CalcUtil.toRoubles(5130L));
        assertEquals(BigDecimal.valueOf(123400).movePointLeft(2), CalcUtil.toRoubles(123400L));
    }

    @Test
    void toRoublesWithScaleTest() {
        assertEquals(BigDecimal.valueOf(0).movePointLeft(2), CalcUtil.toRoublesWithScale(0L));
        assertEquals(BigDecimal.valueOf(1).movePointLeft(2), CalcUtil.toRoublesWithScale((1L)));
        assertEquals(BigDecimal.valueOf(311).movePointLeft(2), CalcUtil.toRoublesWithScale(311L));
        assertEquals(BigDecimal.valueOf(5130).movePointLeft(2), CalcUtil.toRoublesWithScale(5130L));
        assertEquals(BigDecimal.valueOf(123400).movePointLeft(2), CalcUtil.toRoublesWithScale(123400L));
    }

    @Test
    void calcDimensionsIntegerSumTest() {
        assertEquals(6L, CalcUtil.calcDimensionsSum(1, 2, 3));
    }

    @Test
    void calcDimensionsSumTest() {
        assertEquals(6L, CalcUtil.calcDimensionsSum(1L, 2L, 3L));
    }
}
