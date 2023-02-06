package ru.yandex.autotests.directintapi.tests.autobudgetalerts.ordersnotexceededbudget;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersNotExceededBudgetParams;
import ru.yandex.autotests.directapi.darkside.model.AlertStatus;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.logic.ppc.AutobudgetAlerts;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.steps.DBSteps;
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
    static DarkSideSteps darkSideSteps = new DarkSideSteps();
    static DBSteps dbSteps = darkSideSteps.getDBSteps();
    LogSteps log = LogSteps.getLogger(this.getClass());
    Long cid;
    Integer orderID;
    Long cid2;
    Integer orderID2;
    Integer problems = 10;
    Integer overdraft = -20;
    Integer problems2 = 100;
    Integer overdraft2 = 200;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


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
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, problems, overdraft)
                        .withAlert(orderID2, problems2, overdraft2)
        );

        log.info("Проверяем, что записи с алертами появились в БД");
        AutobudgetAlerts dbElement =
                dbSteps.getAutobudgetAlertsSteps().getAlert(cid);
        AutobudgetAlerts expected = new AutobudgetAlerts();
        expected.setProblems(problems);
        expected.setOverdraft((long) overdraft);
        expected.setStatus(AlertStatus.ACTIVE.toString());
        assertThat("Алерт для первой кампании не появился в БД или данные не совпали",
                dbElement,
                beanEquals(expected));

        AutobudgetAlerts dbElement2 =
                dbSteps.getAutobudgetAlertsSteps().getAlert(cid2);
        AutobudgetAlerts expected2 = new AutobudgetAlerts();
        expected2.setProblems(problems2);
        expected2.setOverdraft((long) overdraft2);
        expected2.setStatus(AlertStatus.ACTIVE.toString());
        assertThat("Алерт для второй кампании не появился в БД или данные не совпали",
                dbElement2,
                beanEquals(expected2));
    }
}
