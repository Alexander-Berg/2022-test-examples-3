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
@Aqua.Test(title = "ordersNotExceededBudget - обновление алерта")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_ALERTS_ORDERS_NOT_EXCEEDED_BUDGET)
public class OrdersNotExceededBudgetUpdateTest {
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private LogSteps log = LogSteps.getLogger(this.getClass());
    private long cid;
    private int orderID;

    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void before() {
        int problems = 10;
        int overdraft = -20;
        log.info("Создаем кампанию c orderID");
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
        log.info("Отправляем алерт на кампанию");
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, problems, overdraft)
        );
    }

    @Test
    public void ordersNotExceededBudgetAddAlertTest() {
        short newProblems = 100;
        int newOverdraft = -200;
        log.info("Отправляем второй алерт на ту же кампанию");
        darkSideSteps.getAutobudgetAlertsSteps().ordersNotExceededBudget(
                new OrdersNotExceededBudgetParams()
                        .withAlert(orderID, (int) newProblems, newOverdraft)
        );

        log.info("Проверяем, что данные алерта обновились в БД");
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
        AutobudgetAlertsRecord testRecord = api.userSteps.getDirectJooqDbSteps().autoBudgetAlertsSteps()
                .getAutobudgetAlertsRecord(cid);
        AutobudgetAlertsRecord expected = new AutobudgetAlertsRecord();
        expected.setCid(cid);
        expected.setOverdraft((long) newOverdraft);
        expected.setProblems(newProblems);
        expected.setStatus(AutobudgetAlertsStatus.active);
        assertThat("Алерт не появился в БД или данные не совпали",
                testRecord,
                beanEquals(expected));
    }
}
