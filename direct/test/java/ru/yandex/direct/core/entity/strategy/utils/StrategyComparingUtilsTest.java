package ru.yandex.direct.core.entity.strategy.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy;
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy;
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy;

import static java.time.LocalDateTime.now;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class StrategyComparingUtilsTest {

    private final LocalDateTime now = now();

    @Test
    public void strategiesDontHaveImportantDifferentProps_WhenEqual() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa();
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa();
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_WhenDifferentTypes() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa();
        var testStrategy2 = TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp();
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_WhenDifferentValuesInImportantProps() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withGoalId(1L);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withGoalId(2L);
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_WhenNotImportantFieldsChanged() {
        var testStrategy1 = TestDefaultManualStrategy.clientDefaultManualStrategy()
                .withId(1L)
                .withName("first")
                .withCids(List.of())
                .withClientId(123L)
                .withIsPublic(false)
                .withWalletId(null)
                .withLastChange(now)
                .withStatusArchived(false)
                .withDayBudgetLastChange(now)
                .withDayBudgetDailyChangeCount(1);
        var testStrategy2 = TestDefaultManualStrategy.clientDefaultManualStrategy()
                .withId(2L)
                .withName(null)
                .withCids(List.of(1L))
                .withClientId(345L)
                .withIsPublic(true)
                .withWalletId(34L)
                .withLastChange(now.plusHours(1))
                .withStatusArchived(true)
                .withDayBudgetLastChange(now.plusHours(1))
                .withDayBudgetDailyChangeCount(2);
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_LastUpdateTimeChangeCheck() {
        var testStrategy1 =
                TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy()
                        .withLastUpdateTime(now);
        var testStrategy2 =
                TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy()
                        .withLastUpdateTime(now.plusHours(1));
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_LastBidderRestartTimeCheck() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withLastBidderRestartTime(now);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withLastBidderRestartTime(now.plusHours(1));
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_NullAndFalseEqualForBooleans() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withIsPayForConversionEnabled(null);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withIsPayForConversionEnabled(false);
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_BooleansAreDifferent() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withIsPayForConversionEnabled(null);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withIsPayForConversionEnabled(true);
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_BigDecimalWithDifferentScale() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withAvgCpa(BigDecimal.valueOf(52));
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withAvgCpa(BigDecimal.valueOf(52.00));
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_MetrikaCountersInDifferentOrder() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(List.of(1L, 2L));
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(List.of(2L, 1L));
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_MetrikaCountersHaveDifferentSize() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(List.of(1L, 2L));
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(List.of(1L));
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_MetrikaCountersIsNullInOneStrategy() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(List.of(1L, 2L));
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(null);
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_MetrikaCountersAreNull() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(null);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMetrikaCounters(null);
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_MeaningfulGoalsInDifferentOrderAndNotEqualByEquals() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(12))
                                .withGoalId(13L)
                                .withIsMetrikaSourceOfValue(null),
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(15.00))
                                .withGoalId(12L)
                                .withIsMetrikaSourceOfValue(false)));
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(15))
                                .withGoalId(12L)
                                .withIsMetrikaSourceOfValue(null),
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(12.00))
                                .withGoalId(13L)
                                .withIsMetrikaSourceOfValue(false)));
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_MeaningfulGoalsDifferentInOneField() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(12))
                                .withGoalId(13L)
                                .withIsMetrikaSourceOfValue(null)));
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(12.00))
                                .withGoalId(13L)
                                .withIsMetrikaSourceOfValue(true)));
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_OneMeaningfulGoalsContainNullInList() {
        List<MeaningfulGoal> list1 = new LinkedList<>();
        list1.add(null);
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(list1);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(
                        new MeaningfulGoal()
                                .withConversionValue(BigDecimal.valueOf(12.00))
                                .withGoalId(13L)
                                .withIsMetrikaSourceOfValue(true)));
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesHaveImportantDifferentProps_OneMeaningfulGoalsAreNull() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(null);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(new MeaningfulGoal()
                        .withConversionValue(BigDecimal.valueOf(12))
                        .withGoalId(13L)
                        .withIsMetrikaSourceOfValue(null)));
        assertTrue(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }

    @Test
    public void strategiesDontHaveImportantDifferentProps_MeaningfulGoalsAreNull() {
        var testStrategy1 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(null);
        var testStrategy2 = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
                .withMeaningfulGoals(null);
        assertFalse(StrategyComparingUtils.areDifferentStrategies(testStrategy1, testStrategy2));
    }
}
