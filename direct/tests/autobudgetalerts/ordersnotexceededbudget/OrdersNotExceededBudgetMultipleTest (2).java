package ru.yandex.autotests.directintapi.tests.autobudgetalerts.ordersnotexceededbudget;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutobudgetAlertsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutobudgetAlertsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersNotExceededBudgetParams;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 17.02.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-1510
 */
@Aqua.Test(title = "ordersNotExceededBudget - две кампании")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_ALERTS_ORDERS_NOT_EXCEEDED_BUDGET)
public class OrdersNotExceededBudgetMultipleTest {
    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);
    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private LogSteps log = LogSteps.getLogger(this.getClass());
    private Long cid;
    private Integer orderID;
    private Long cid2;
    private Integer orderID2;

    @Before
    public void before() {
        log.info("Создаем две кампании c orderID");
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
        cid2 = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderID2 = api.userSteps.campaignFakeSteps().setRandomOrderID(cid2);
    }

    @Test
    public void ordersNotExceededBudgetAddAlertTest() {
        short problems = 10;
        short problems2 = 100;
        int overdraft = -20;
        int overdraft2 = 200;
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, (int) problems, overdraft)
                        .withAlert(orderID2, (int) problems2, overdraft2)
        );

        log.info("Проверяем, что записи с алертами появились в БД");
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
        AutobudgetAlertsRecord testRecord = api.userSteps.getDirectJooqDbSteps().autoBudgetAlertsSteps()
                .getAutobudgetAlertsRecord(cid);
        AutobudgetAlertsRecord expected = new AutobudgetAlertsRecord();
        expected.setCid(cid);
        expected.setOverdraft((long) overdraft);
        expected.setProblems(problems);
        expected.setStatus(AutobudgetAlertsStatus.active);
        assertThat("Алёрт для первой кампании не появился в БД или данные не совпали",
                testRecord,
                beanEquals(expected));

        AutobudgetAlertsRecord testRecord2 = api.userSteps.getDirectJooqDbSteps().autoBudgetAlertsSteps()
                .getAutobudgetAlertsRecord(cid2);
        AutobudgetAlertsRecord expected2 = new AutobudgetAlertsRecord();
        expected.setCid(cid2);
        expected.setOverdraft((long) overdraft2);
        expected.setProblems(problems);
        expected.setStatus(AutobudgetAlertsStatus.active);
        assertThat("Алёрт для второй кампании не появился в БД или данные не совпали",
                testRecord2,
                beanEquals(expected2));
    }
}
