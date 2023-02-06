package ru.yandex.direct.utils.math;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class MathUtilsCalcPercentileTest {
    private static final List<Integer> VALS = asList(65, 234, 742, 7, 12, 24);
    private static final double PERCENTAGE = 0.5;
    private static final double EXPECTED_VALUE = 44.5;

    @Test
    public void calPercentile_Positive_with_integers() {
        double percentile = MathUtils.calcQuantile(VALS, PERCENTAGE);
        Assertions.assertThat(percentile).isEqualByComparingTo(EXPECTED_VALUE);
    }

    @Test
    public void calPercentile_Positive_with_BibDecimal() {
        List<BigDecimal> bidDecimalVals = VALS.stream()
                .map(BigDecimal::valueOf)
                .collect(Collectors.toList());
        double percentile = MathUtils.calcQuantile(bidDecimalVals, PERCENTAGE);
        Assertions.assertThat(percentile).isEqualByComparingTo(EXPECTED_VALUE);
    }

    @Test
    public void calPercentile_Empty_values() {
        double percentile = MathUtils.calcQuantile(emptyList(), PERCENTAGE);
        Assertions.assertThat(percentile).isEqualByComparingTo(0d);
    }

    @Test(expected = IllegalStateException.class)
    public void calPercentile_Invalid_percentile() {
        double invalidPercentile = 2;
        MathUtils.calcQuantile(VALS, invalidPercentile);
    }

    @Test
    public void calPercentile_zero_percentile() {
        double percentile = MathUtils.calcQuantile(asList(1, 2, 3), 0);
        Assertions.assertThat(percentile).isEqualByComparingTo(1d);
    }

    @Test
    public void calPercentile_Positive_30_percentile() {
        double percentile = MathUtils.calcQuantile(VALS, 0.3);
        Assertions.assertThat(percentile).isEqualByComparingTo(18d);
    }

    @Test
    public void calPercentile_40_percentile() {
        double percentile = MathUtils.calcQuantile(asList(15, 20, 35, 40, 50), 0.4);
        Assertions.assertThat(percentile).isEqualByComparingTo(29d);
    }

    @Test
    public void calPercentile_75_percentile() {
        double percentile = MathUtils.calcQuantile(asList(1, 2, 3, 4), 0.75);
        Assertions.assertThat(percentile).isEqualByComparingTo(3.25);
    }

    @Test
    public void calPercentile_one_percentile() {
        double percentile = MathUtils.calcQuantile(asList(1, 2, 3), 1);
        Assertions.assertThat(percentile).isEqualByComparingTo(3d);
    }
}
