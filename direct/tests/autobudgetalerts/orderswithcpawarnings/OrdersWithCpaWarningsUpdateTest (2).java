package ru.yandex.autotests.directintapi.tests.autobudgetalerts.orderswithcpawarnings;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutobudgetCpaAlertsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutobudgetCpaAlertsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.OrdersWithCpaWarningsParams;
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
@Aqua.Test(title = "ordersWithCpaWarnings - обновление алерта")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_ALERTS_ORDERS_WITH_CPA_WARNINGS)
public class OrdersWithCpaWarningsUpdateTest {
    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);
    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private LogSteps log = LogSteps.getLogger(this.getClass());
    private long cid;
    private int orderID;

    @Before
    public void before() {
        int cpa = 1000;
        int apc = 2000;
        log.info("Создаем кампанию c orderID");
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
        log.info("Отправляем алерт на кампанию");
        darkSideSteps.getAutobudgetAlertsSteps().ordersWithCpaWarnings(
                new OrdersWithCpaWarningsParams()
                        .withAlert(orderID, cpa, apc)
        );
    }

    @Test
    public void ordersWithCpaWarningsAddAlertTest() {
        int cpaNew = 3000;
        int apcNew = 4000;
        log.info("Отправляем новый алерт на ту же кампанию");
        darkSideSteps.getAutobudgetAlertsSteps().ordersWithCpaWarnings(
                new OrdersWithCpaWarningsParams()
                        .withAlert(orderID, cpaNew, apcNew)
        );

        log.info("Проверяем, что данные алерта обновились в БД");
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
        AutobudgetCpaAlertsRecord testRecord =
                api.userSteps.getDirectJooqDbSteps().autoBudgetCpaAlertsSteps().getAutobudgetCpaAlertsRecord(cid);
        AutobudgetCpaAlertsRecord expected = new AutobudgetCpaAlertsRecord();
        expected.setCid(cid);
        expected.setCpaDeviation(BigInteger.valueOf(cpaNew));
        expected.setApcDeviation(BigInteger.valueOf(apcNew));
        expected.setStatus(AutobudgetCpaAlertsStatus.active);
        assertThat("Алерт не появился в БД или данные не совпали",
                testRecord,
                beanEquals(expected));
    }
}
