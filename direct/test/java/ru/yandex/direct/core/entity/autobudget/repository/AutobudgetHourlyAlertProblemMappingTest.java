package ru.yandex.direct.core.entity.autobudget.repository;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.jooq.types.UShort;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class AutobudgetHourlyAlertProblemMappingTest {

    @Parameterized.Parameter(0)
    public int expectedDbProblem;

    @Parameterized.Parameter(1)
    public EnumSet<AutobudgetHourlyProblem> expectedHourlyAlertProblems;

    @Parameterized.Parameters(name = "{1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[]{0, EnumSet.noneOf(AutobudgetHourlyProblem.class)},
                new Object[]{1, EnumSet.of(AutobudgetHourlyProblem.IN_ROTATION)},
                new Object[]{2, EnumSet.of(AutobudgetHourlyProblem.MAX_BID_REACHED)},
                new Object[]{4, EnumSet.of(AutobudgetHourlyProblem.MARGINAL_PRICE_REACHED)},
                new Object[]{8, EnumSet.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED)},
                new Object[]{16, EnumSet.of(AutobudgetHourlyProblem.ENGINE_MIN_COST_LIMITED)},
                new Object[]{32, EnumSet.of(AutobudgetHourlyProblem.LIMITED_BY_BALANCE)},
                new Object[]{64, EnumSet.of(AutobudgetHourlyProblem.NO_BANNERS)},
                new Object[]{128, EnumSet.of(AutobudgetHourlyProblem.LIMIT_BY_AVG_COST)},
                new Object[]{256, EnumSet.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED)},

                new Object[]{256 + 32 + 2,
                        EnumSet.of(
                                AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED,
                                AutobudgetHourlyProblem.LIMITED_BY_BALANCE,
                                AutobudgetHourlyProblem.MAX_BID_REACHED
                        )}
        );
    }

    @Test
    public void problemsFromDb_returnValue() {
        Set<AutobudgetHourlyProblem> modelProblems = AutobudgetMapping.problemsFromDb(
                UShort.valueOf(expectedDbProblem));
        assertThat("Сконвертированная из БД автобюджетная проблема не совпадает с ожидаемой",
                modelProblems, equalTo(expectedHourlyAlertProblems));
    }

    @Test
    public void problemsToDb_returnValue() {
        UShort dbProblems = AutobudgetMapping.problemsToDb(expectedHourlyAlertProblems);
        assertThat("Сконвертированная в БД автобюджетная проблема не совпадает с ожидаемой",
                dbProblems.intValue(), equalTo(expectedDbProblem));
    }
}
