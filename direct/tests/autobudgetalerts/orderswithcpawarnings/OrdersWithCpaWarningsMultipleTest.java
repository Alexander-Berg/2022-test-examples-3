package ru.yandex.autotests.directintapi.tests.autobudgetalerts.orderswithcpawarnings;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersWithCpaWarningsParams;
import ru.yandex.autotests.directapi.darkside.model.AlertStatus;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.logic.ppc.AutobudgetCpaAlerts;
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
@Aqua.Test(title = "ordersWithCpaWarnings - добавление алерта")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_ALERTS_ORDERS_WITH_CPA_WARNINGS)
public class OrdersWithCpaWarningsMultipleTest {
    static DarkSideSteps darkSideSteps = new DarkSideSteps();
    static DBSteps dbSteps = darkSideSteps.getDBSteps();
    LogSteps log = LogSteps.getLogger(this.getClass());
    Long cid;
    Integer orderID;
    Long cid2;
    Integer orderID2;
    Integer cpa = 1000;
    Integer apc = 2000;
    Integer cpa2 = 3000;
    Integer apc2 = 4000;

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
    public void ordersWithCpaWarningsAddAlertTest() {
        darkSideSteps.getAutobudgetAlertsSteps().ordersWithCpaWarnings(
                new OrdersWithCpaWarningsParams()
                        .withAlert(orderID, cpa, apc)
                        .withAlert(orderID2, cpa2, apc2)
        );

        log.info("Проверяем, что записи с алертами появились в БД");
        AutobudgetCpaAlerts dbElement =
                dbSteps.getAutobudgetAlertsSteps().getCpaAlert(cid);
        AutobudgetCpaAlerts expected = new AutobudgetCpaAlerts();
        expected.setCpaDeviation(BigInteger.valueOf(cpa));
        expected.setApcDeviation(BigInteger.valueOf(apc));
        expected.setStatus(AlertStatus.ACTIVE.toString());
        assertThat("Алерт не появился в БД или данные не совпали",
                dbElement,
                beanEquals(expected));

        AutobudgetCpaAlerts dbElement2 =
                dbSteps.getAutobudgetAlertsSteps().getCpaAlert(cid2);
        AutobudgetCpaAlerts expected2 = new AutobudgetCpaAlerts();
        expected2.setCpaDeviation(BigInteger.valueOf(cpa2));
        expected2.setApcDeviation(BigInteger.valueOf(apc2));
        expected2.setStatus(AlertStatus.ACTIVE.toString());
        assertThat("Алерт не появился в БД или данные не совпали",
                dbElement2,
                beanEquals(expected2));
    }
}
