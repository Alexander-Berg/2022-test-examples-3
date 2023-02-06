package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

import java.util.Arrays;
import java.util.Collection;

//TESTIRT-9321
@Aqua.Test
@Description("Уведомления по стратегиям для текстовых кампаний отправленные intapi-ручкой")
@Features(TestFeatures.AUTOBUDGET_ALERTS)
@Tag(CampTypeTag.TEXT)
@Tag(BusinessProcessTag.ALERT)
@RunWith(Parameterized.class)
public class AddTextAutobudgetAlertsTest extends AutobudgetAlertsBaseTest {
    @Parameterized.Parameter
    public Integer problems;
    @Parameterized.Parameter(value = 1)
    public Short expProblems;

    @Parameterized.Parameters(name = "отправляем причины {0} ожидаем причины {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {PROBLEM_I_1, (short) 2},
                {PROBLEM_I_2, (short) 4},
                {PROBLEM_I_1 + PROBLEM_I_2, (short) 6},
                {PROBLEM_II, (short) 8},
                {PROBLEM_III, (short) 16},
                {PROBLEM_I_1 + PROBLEM_II, (short) 10},
                {PROBLEM_I_2 + PROBLEM_II, (short) 12},
                {PROBLEM_I_1 + PROBLEM_III, (short) 18},
                {PROBLEM_I_2 + PROBLEM_III, (short) 20},
                {PROBLEM_II + PROBLEM_III, (short) 24},
                {PROBLEM_I_1 + PROBLEM_II + PROBLEM_III, (short) 26},
                {PROBLEM_I_2 + PROBLEM_II + PROBLEM_III, (short) 28},
        });
    }

    @Override
    BannersRule getBannerRule() {
        if (bannersRule == null) {
            bannersRule = new TextBannersRule()
                    .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT))
                    .withUlogin(CLIENT);
        }
        return bannersRule;
    }

    @Override
    int getProblems() {
        return problems;
    }

    @Override
    Short getExpProblems() {
        return expProblems;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9004")
    public void setAndCheckProblems() {
        super.setAndCheckProblems();
    }
}
