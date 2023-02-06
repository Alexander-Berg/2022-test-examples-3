package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsOptionsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - рублевый клиент")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2CurrencyTest {

    private static LogSteps log = LogSteps.getLogger(NotifyClient2CurrencyTest.class);

    @ClassRule
    public static ApiSteps api = new ApiSteps();
    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static Long clientID;

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqDbSteps;

    @BeforeClass
    public static void initTestData() {
        jooqDbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_RUB3);
        clientID = jooqDbSteps.usersSteps().getUser(Logins.LOGIN_RUB3).getClientid();
    }


    @Test
    public void notifyRubClientChangeOverdraftLimitTest() {
        Money overdraftLimit = Money.valueOf(100.0, Currency.RUB);
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение лимита овердрафта.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit.floatValue())
                        .withOverdraftSpent(0f)
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение overdraftLim не соответствует ожидаемому",
                clientsOptions.getOverdraftLim(), equalTo(overdraftLimit.bigDecimalValue().setScale(2)));
    }

    @Test
    public void notifyRubClientChangeOverdraftLimitTestToZero() {
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение лимита овердрафта на 0.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(100.0f)
                        .withOverdraftSpent(0f)
                        .withClientCurrency(Currency.RUB.toString())
        );

        BigDecimal zeroOverdraftLimit = BigDecimal.ZERO;
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(zeroOverdraftLimit.floatValue())
                        .withOverdraftSpent(0f)
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение overdraftLim не соответствует ожидаемому",
                clientsOptions.getOverdraftLim(), equalTo(zeroOverdraftLimit.setScale(2)));
    }

    @Test
    public void notifyRubClientChangeDebtTest() {
        BigDecimal debt = BigDecimal.valueOf(100.0f);
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение суммы задолженности.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(debt.floatValue())
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение debt не соответствует ожидаемому",
                clientsOptions.getDebt(), equalTo(debt.setScale(2)));
    }

    @Test
    public void notifyRubClientChangeDebtToZeroTest() {
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение суммы задолженности на 0.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(100.0f)
                        .withClientCurrency(Currency.RUB.toString())
        );

        BigDecimal zeroDebt = BigDecimal.ZERO;
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(zeroDebt.floatValue())
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение debt не соответствует ожидаемому",
                clientsOptions.getDebt(), equalTo(zeroDebt.setScale(2)));
    }

    @Test
    public void notifyRubClientChangeDebtLessThanOverdraftTest() {
        Money overdraftLimit = Money.valueOf(150.0f, Currency.RUB);
        Money debt = Money.valueOf(100.0f, Currency.RUB);
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение суммы задолженности и лимита овердрафта. Задолженность меньше лимита.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit.floatValue())
                        .withOverdraftSpent(debt.floatValue())
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение overdraftLim не соответствует ожидаемому",
                clientsOptions.getOverdraftLim(), equalTo(overdraftLimit.bigDecimalValue().setScale(2)));
        assertThat("Значение debt не соответствует ожидаемому",
                clientsOptions.getDebt(), equalTo(debt.bigDecimalValue().setScale(2)));
    }

    @Test
    public void notifyRubClientChangeDebtEqualToOverdraftTest() {
        Money overdraftLimit = Money.valueOf(100.0f, Currency.RUB);
        Money debt = Money.valueOf(100.0f, Currency.RUB);
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение суммы задолженности и лимита овердрафта. Задолженность равна лимиту.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit.floatValue())
                        .withOverdraftSpent(debt.floatValue())
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение overdraftLim не соответствует ожидаемому",
                clientsOptions.getOverdraftLim(), equalTo(overdraftLimit.bigDecimalValue().setScale(2)));
        assertThat("Значение debt не соответствует ожидаемому",
                clientsOptions.getDebt(), equalTo(debt.bigDecimalValue().setScale(2)));
    }

    @Test
    public void notifyRubClientChangeDebtMoreThanOverdraftTest() {
        Money overdraftLimit = Money.valueOf(100.0f, Currency.RUB);
        Money debt = Money.valueOf(150.0f, Currency.RUB);
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение суммы задолженности и лимита овердрафта. Задолженность больше лимита.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit.floatValue())
                        .withOverdraftSpent(debt.floatValue())
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение overdraftLim не соответствует ожидаемому",
                clientsOptions.getOverdraftLim(), equalTo(overdraftLimit.bigDecimalValue().setScale(2)));
        assertThat("Значение debt не соответствует ожидаемому",
                clientsOptions.getDebt(), equalTo(debt.bigDecimalValue().setScale(2)));
    }

    @Test
    public void notifyRubClientChangeNextPayDateTest() {
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение даты платежа.");
        String date = sdf.format(DateTime.now().plusDays(1).toDate());
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0.01f)
                        .withClientCurrency(Currency.RUB.toString())
                        .withMinPaymentTerm(date)
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение nextPayDate не соответствует ожидаемому",
                sdf.format(clientsOptions.getNextpaydate()), equalTo(date));
    }

    @Test
    public void notifyRubClientChangeNextPayDateToOldTest() {
        log.info("Вызываем метод NotifyClient для рублевого клиента. Проверяем изменение даты платежа на просроченную.");
        String date = sdf.format(DateTime.now().minus(1).toDate());
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0.01f)
                        .withMinPaymentTerm(date)
                        .withClientCurrency(Currency.RUB.toString())
        );

        ClientsOptionsRecord clientsOptions = jooqDbSteps.clientsOptionsSteps().getClientOptions(clientID);
        assertThat("Значение nextPayDate не соответствует ожидаемому",
                sdf.format(clientsOptions.getNextpaydate()), equalTo(date));
        //TODO что ещё должно происходить при просроченном платеже?
    }
}
