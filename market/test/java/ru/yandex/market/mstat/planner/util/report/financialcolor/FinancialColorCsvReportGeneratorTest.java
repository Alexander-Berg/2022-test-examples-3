package ru.yandex.market.mstat.planner.util.report.financialcolor;

import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.mstat.planner.model.FinancialColourCode;
import ru.yandex.market.mstat.planner.model.ProjectColor;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FinancialColorCsvReportGeneratorTest {

    @Test
    public void twoColorsGiveHundred() {
        Map<FinancialColourCode, Integer> percentMap = FinancialColorCsvReportGenerator.percentMap(ImmutableSet.of(ProjectColor.r, ProjectColor.w));
        assertThat(percentMap.keySet().size(), is(2));
        assertThat(percentMap.keySet(), hasItems(FinancialColourCode.AD206, FinancialColourCode.AD770));
        assertThat(percentMap.values().stream().mapToInt(Integer::intValue).sum(), is (100));
    }

    @Test
    public void threeColorsGiveHundred() {
        Map<FinancialColourCode, Integer> percentMap = FinancialColorCsvReportGenerator.percentMap(ImmutableSet.of(
                ProjectColor.r, ProjectColor.w, ProjectColor.b));
        assertThat(percentMap.keySet().size(), is(3));
        assertThat(percentMap.keySet(), hasItems(FinancialColourCode.AD206, FinancialColourCode.AD770, FinancialColourCode.AD205));
        assertThat(percentMap.values().stream().mapToInt(Integer::intValue).sum(), is (100));
        assertThat(percentMap.get(FinancialColourCode.AD205), is (34));
        assertThat(percentMap.get(FinancialColourCode.AD206), is (33));
        assertThat(percentMap.get(FinancialColourCode.AD770), is (33));
    }

    @Test
    public void sevenColorsGiveHundred() {
        Map<FinancialColourCode, Integer> percentMap = FinancialColorCsvReportGenerator.percentMap(ImmutableSet.of(
                ProjectColor.r, ProjectColor.w,ProjectColor.o, ProjectColor.b,
                ProjectColor.dp, ProjectColor.ff, ProjectColor.lb));
        assertThat(percentMap.keySet().size(), is(7));
        assertThat(percentMap.keySet(), hasItems(FinancialColourCode.AD206, FinancialColourCode.AD770, FinancialColourCode.AD205, FinancialColourCode.AD200));
        assertThat(percentMap.values().stream().mapToInt(Integer::intValue).sum(), is (100));
        assertThat(percentMap.get(FinancialColourCode.AD205), is (15));
        assertThat(percentMap.get(FinancialColourCode.AD206), is (15));
        assertThat(percentMap.get(FinancialColourCode.AD770), is (14));
        assertThat(percentMap.get(FinancialColourCode.AD200), is (14));
        assertThat(percentMap.get(FinancialColourCode.AD208), is (14));
        assertThat(percentMap.get(FinancialColourCode.AD701), is (14));
        assertThat(percentMap.get(FinancialColourCode.AD702), is (14));
    }

    @Test
    public void sixColorsGiveHundred() {
        Map<FinancialColourCode, Integer> percentMap = FinancialColorCsvReportGenerator.percentMap(ImmutableSet.of(
                ProjectColor.r, ProjectColor.b,
                ProjectColor.lb, ProjectColor.lw, ProjectColor.lr,
                ProjectColor.ff));
        assertThat(percentMap.keySet().size(), is(6));
        assertThat(percentMap.values().stream().mapToInt(Integer::intValue).sum(), is (100));
        assertThat(percentMap.get(FinancialColourCode.AD205), is (17));
        assertThat(percentMap.get(FinancialColourCode.AD701), is (17));
        assertThat(percentMap.get(FinancialColourCode.AD702), is (17));
        assertThat(percentMap.get(FinancialColourCode.AD710), is (16));
        assertThat(percentMap.get(FinancialColourCode.AD716), is (16));
        assertThat(percentMap.get(FinancialColourCode.AD770), is (17));
    }

}
