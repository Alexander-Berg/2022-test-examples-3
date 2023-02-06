package ru.yandex.autotests.direct.cmd.autobudgetalerts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutobudgetAlertsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutobudgetAlertsRecord;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersNotExceededBudgetParams;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;

import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

//TESTIRT-9321
public abstract class AutobudgetAlertsBaseTest {
    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static final String CLIENT = "at-direct-smart-alert-1";
    static final int PROBLEM_I_1 = 2;
    static final int PROBLEM_I_2 = 4;
    static final int PROBLEM_II = 8;
    static final int PROBLEM_III = 16;
    static final int OVERDRAFT = -20;
    protected DarkSideSteps darkSideSteps;
    protected Integer cid;
    protected BannersRule bannersRule = getBannerRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    Integer orderID;

    abstract BannersRule getBannerRule();

    abstract int getProblems();

    abstract Short getExpProblems();

    @Before
    public void before() {
        darkSideSteps = cmdRule.apiSteps().getDarkSideSteps();
        Long campaignId = bannersRule.getCampaignId();
        cid = campaignId.intValue();
        orderID = cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(cid);
    }

    public void setAndCheckProblems() {
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, getProblems(), OVERDRAFT)
        );
        cmdRule.apiSteps().getDirectJooqDbSteps().useShardForLogin(CLIENT);
        AutobudgetAlertsRecord testRecord = cmdRule.apiSteps().getDirectJooqDbSteps().autoBudgetAlertsSteps()
                .getAutobudgetAlertsRecord((long) cid);
        AutobudgetAlertsRecord expected = new AutobudgetAlertsRecord();
        expected.setCid((long) cid);
        expected.setStatus(AutobudgetAlertsStatus.active);
        expected.setOverdraft((long) OVERDRAFT);
        expected.setProblems(getExpProblems());
        assertThat("Причина алерта сохранена в БД и соответствует ожиданиям",
                testRecord,
                recordDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

}
