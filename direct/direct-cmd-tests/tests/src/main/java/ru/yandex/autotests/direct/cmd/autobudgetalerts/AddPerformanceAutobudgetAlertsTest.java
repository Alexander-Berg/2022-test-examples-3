package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutobudgetAlertsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutobudgetAlertsRecord;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersNotExceededBudgetParams;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Уведомления по стратегиям для смарт-кампаний отправленные intapi-ручкой")
@Features(TestFeatures.AUTOBUDGET_ALERTS)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(BusinessProcessTag.ALERT)
@RunWith(Parameterized.class)
public class AddPerformanceAutobudgetAlertsTest extends AutobudgetAlertsBaseTest {
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
                {PROBLEM_II, null}, //причина II не должна приходить из БК для смарта //DIRECT-53337
                {PROBLEM_III, (short) 16},
                {PROBLEM_I_1 + PROBLEM_III, (short) 18},
                {PROBLEM_I_2 + PROBLEM_III, (short) 20},
        });
    }

    @Override
    BannersRule getBannerRule() {
        if (bannersRule == null) {
            bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
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
    @ru.yandex.qatools.allure.annotations.TestCaseId("9003")
    public void setAndCheckProblems() {
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, problems, OVERDRAFT)
        );
        cmdRule.apiSteps().getDirectJooqDbSteps().useShardForLogin(CLIENT);
        AutobudgetAlertsRecord testRecord = cmdRule.apiSteps().getDirectJooqDbSteps().autoBudgetAlertsSteps()
                .getAutobudgetAlertsRecord((long) cid);
        if (problems == PROBLEM_II) {
            assumeThat("причина №2 \"Достигнута максимальная позиция выдачи\" не должна сохраняться",
                    testRecord, nullValue());
        } else {
            AutobudgetAlertsRecord expected = new AutobudgetAlertsRecord();
            expected.setCid((long) cid);
            expected.setStatus(AutobudgetAlertsStatus.active);
            expected.setOverdraft((long) OVERDRAFT);
            expected.setProblems(expProblems);
            assumeThat("Причина алерта сохранена в БД и соответствует ожиданиям",
                    testRecord,
                    recordDiffer(expected).useCompareStrategy(onlyExpectedFields()));
        }
    }
}
