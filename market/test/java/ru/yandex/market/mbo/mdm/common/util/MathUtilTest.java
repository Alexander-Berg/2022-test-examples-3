package ru.yandex.market.mbo.mdm.common.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MathUtilTest {

    private static void computeAndCheckGeometricMean(Random random, int valuesNumber, double min, double max) {
        computeAndCheckMean(random, valuesNumber, min, max,
            MathUtil::computeGeometricMeanUsingLogarithms,
            MathUtilTest::checkGeometricMean);
    }

    private static void computeAndCheckArithmeticMean(Random random, int valuesNumber, double min, double max) {
        computeAndCheckMean(random, valuesNumber, min, max,
            MathUtil::arithmeticMean,
            MathUtilTest::checkArithmeticMean);
    }

    private static void computeAndCheckMean(Random random, int valuesNumber, double min, double max,
                                            Function<List<BigDecimal>, Optional<BigDecimal>> computer,
                                            BiFunction<List<BigDecimal>, BigDecimal, Boolean> checker) {
        List<BigDecimal> testValues = generateListOfBigDecimals(random, valuesNumber, min, max);
        Optional<BigDecimal> result = computer.apply(testValues);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(checker.apply(testValues, result.get())).isTrue();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static boolean checkGeometricMean(List<BigDecimal> values, BigDecimal geometricMean) {
        BigDecimal expected = values.stream().reduce(BigDecimal::multiply).get();
        BigDecimal actual = geometricMean.pow(values.size());
        return MathUtil.areClose(actual, expected);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static boolean checkArithmeticMean(List<BigDecimal> values, BigDecimal arithmeticMean) {
        BigDecimal expected = values.stream().reduce(BigDecimal::add).get();
        BigDecimal actual = arithmeticMean.multiply(BigDecimal.valueOf(values.size()));
        return MathUtil.areCloseStrict(expected, actual);
    }

    private static List<BigDecimal> generateListOfBigDecimals(Random random, int listSize, double min, double max) {
        return random.doubles(listSize, min, max)
            .mapToObj(BigDecimal::valueOf)
            .collect(Collectors.toList());
    }

    @Test
    public void testComputeGeometricMeanUsingLogarithms() {
        Random random = new Random(2718281828459045235L);
        // Small numbers (1-2 mg when compute dimensions)
        computeAndCheckGeometricMean(random, 1, 0.000001, 0.000002);
        computeAndCheckGeometricMean(random, 10, 0.000001, 0.000002);
        computeAndCheckGeometricMean(random, 1000, 0.000001, 0.000002);
        computeAndCheckGeometricMean(random, 10000, 0.000001, 0.000002);
        // Large numbers (10-20 tonnes when compute dimensions)
        computeAndCheckGeometricMean(random, 1, 10000, 20000);
        computeAndCheckGeometricMean(random, 10, 10000, 20000);
        computeAndCheckGeometricMean(random, 1000, 10000, 20000);
        computeAndCheckGeometricMean(random, 10000, 10000, 20000);
        // Combined
        computeAndCheckGeometricMean(random, 1, 0.000001, 20000);
        computeAndCheckGeometricMean(random, 10, 0.000001, 20000);
        computeAndCheckGeometricMean(random, 1000, 0.000001, 20000);
        computeAndCheckGeometricMean(random, 10000, 0.000001, 20000);
    }

    @Test
    public void testComputeArithmeticMean() {
        Random random = new Random(3141592L);

        computeAndCheckArithmeticMean(random, 10, 1, 10);
        computeAndCheckArithmeticMean(random, 100, 0.001, 1000);
        computeAndCheckArithmeticMean(random, 10000, 0.000_001, 1_000_000);
        computeAndCheckArithmeticMean(random, 100000, 1e-9, 1e9);
        computeAndCheckArithmeticMean(random, 100000, -100500, 100500);

        Assertions.assertThat(MathUtil.arithmeticMean(null)).isEmpty();
        Assertions.assertThat(MathUtil.arithmeticMean(Collections.emptyList())).isEmpty();
    }

    @Test
    public void testFindMedian() {
        List<BigDecimal> values1 = Stream.of("10", "20", "30")
            .map(BigDecimal::new)
            .collect(Collectors.toList());
        Assertions.assertThat(MathUtil.median(values1)).contains(new BigDecimal("20"));

        List<BigDecimal> values2 = Stream.of(
            "321",
            "657",
            "321",
            "97584",
            "897",
            "0.0001",
            "0.00000001"
        )
            .map(BigDecimal::new)
            .collect(Collectors.toList());
        Assertions.assertThat(MathUtil.median(values2)).contains(new BigDecimal("321"));

        List<BigDecimal> values3 = Stream.of("10", "20", "30", "40")
            .map(BigDecimal::new)
            .collect(Collectors.toList());
        Assertions.assertThat(MathUtil.median(values3)).contains(new BigDecimal("25"));

        List<BigDecimal> values4 = Stream.of("-10", "-20", "-30", "-0.123456789", "0", "0.00001", "546", "789")
            .map(BigDecimal::new)
            .collect(Collectors.toList());
        Assertions.assertThat(MathUtil.median(values4)).contains(new BigDecimal("-0.0617283945"));

        Assertions.assertThat(MathUtil.median(null)).isEmpty();
        Assertions.assertThat(MathUtil.median(Collections.emptyList())).isEmpty();
    }

    @Test
    public void testFitInInt() {
        Assertions.assertThat(MathUtil.fitInInt(Long.MIN_VALUE)).isFalse();
        Assertions.assertThat(MathUtil.fitInInt(-(1L << 45))).isFalse();
        Assertions.assertThat(MathUtil.fitInInt(Integer.MIN_VALUE - 1L)).isFalse();
        Assertions.assertThat(MathUtil.fitInInt(Integer.MIN_VALUE)).isTrue();
        Assertions.assertThat(MathUtil.fitInInt(-100500L)).isTrue();
        Assertions.assertThat(MathUtil.fitInInt(0L)).isTrue();
        Assertions.assertThat(MathUtil.fitInInt(100500L)).isTrue();
        Assertions.assertThat(MathUtil.fitInInt(Integer.MAX_VALUE)).isTrue();
        Assertions.assertThat(MathUtil.fitInInt(Integer.MAX_VALUE + 1L)).isFalse();
        Assertions.assertThat(MathUtil.fitInInt(1L << 45)).isFalse();
        Assertions.assertThat(MathUtil.fitInInt(Long.MAX_VALUE)).isFalse();
    }
}
