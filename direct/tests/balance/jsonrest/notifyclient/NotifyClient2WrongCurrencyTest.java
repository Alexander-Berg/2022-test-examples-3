package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsOptionsRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.ClientInfo;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.logic.ppc.ClientsOptions;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 06.04.15.
 */
@Aqua.Test(title = "NotifyClient2 - валюта, отличная от валюты клиента")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2WrongCurrencyTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyClient2WrongCurrencyTest.class);
    private static Long clientID;
    private static String login;
    private static Long rubClientID;
    private static String rubLogin;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void initTestData() {
        darkSideSteps = new DarkSideSteps();

        log.info("Создаем клиента для теста");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient9-", Logins.LOGIN_MNGR);
        clientID = (long) clientInfo.getClientID();
        login = clientInfo.getLogin();

        api.as(Logins.LOGIN_SUPER);
        rubLogin = Logins.LOGIN_RUB2;
        rubClientID = Long.valueOf(darkSideSteps.getClientFakeSteps().getClientData(rubLogin).getClientID());
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(rubLogin).clientsOptionsSteps().dropClientRecord(rubClientID);
    }


    @Test
    public void notifyNonCurrencyClientWithRubCurrencyTest() {
        darkSideSteps.getDBSteps().getClientsSteps().deleteClientsOptions(clientID.intValue());
        log.info("Вызываем метод NotifyClient для обычного клиента. Запрос в валюте.");

        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0.01f)
                        .withMinPaymentTerm("2013-09-01")
                        .withClientCurrency(Currency.RUB.toString()),
                500, -32603, "Currency from balance RUB doesn't match client currency YND_FIXED for ClientID"
        );

        ClientInfo clientInfo = api.userSteps.clientSteps().getClientInfo(login);
        assertThat("Значение overdraftSumAvailable не изменилось", clientInfo.getOverdraftSumAvailableInCurrency(),
                equalTo(0.0f));

        ClientsOptions clientsOptions = darkSideSteps.getDBSteps().getClientsSteps().getClientsOptions(clientID);
        assertThat("В таблице ClientsOptions не должно быть записи для клиента", clientsOptions, nullValue());
    }

    @Test
    public void notifyRubClientWithoutCurrencyTest() {
        float overdraftLimit = 100.0f;
        log.info("Вызываем метод NotifyClient для рублевого клиента. Запрос в фишках.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(rubClientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit)
                        .withOverdraftSpent(0f),
                500, -32603,
                "Currency from balance YND_FIXED doesn't match client currency RUB for ClientID"
        );
        ClientInfo clientInfo = api.userSteps.clientSteps().getClientInfo(rubLogin);
        assertThat("Значение overdraftSumAvailable изменилось", clientInfo.getOverdraftSumAvailableInCurrency(),
                equalTo(0.0f));

        ClientsOptionsRecord clientsOptions = api.userSteps.getDirectJooqDbSteps().clientsOptionsSteps().getClientOptions(rubClientID);
        assertThat("В таблице ClientsOptions не должно быть записи для клиента", clientsOptions, nullValue());
    }

    @Test
    public void notifyRubClientWithNonRubCurrencyTest() {
        float overdraftLimit = 100.0f;
        log.info("Вызываем метод NotifyClient для рублевого клиента. Запрос в евро.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(rubClientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit)
                        .withOverdraftSpent(0f)
                        .withClientCurrency("EUR"),

                500, -32603,
                "Currency from balance EUR doesn't match client currency RUB for ClientID"
        );
        ClientInfo clientInfo = api.userSteps.clientSteps().getClientInfo(rubLogin);
        assertThat("Значение overdraftSumAvailable изменилось", clientInfo.getOverdraftSumAvailableInCurrency(),
                equalTo(0.0f));

        ClientsOptionsRecord clientsOptions = api.userSteps.getDirectJooqDbSteps().clientsOptionsSteps().getClientOptions(rubClientID);
        assertThat("В таблице ClientsOptions не должно быть записи для клиента", clientsOptions, nullValue());
    }

    @Test
    public void notifyRubClientWithNonexistingCurrencyTest() {
        float overdraftLimit = 100.0f;
        log.info("Вызываем метод NotifyClient для рублевого клиента. Запрос в валюте, с которой не умеем работать.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(rubClientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit)
                        .withOverdraftSpent(0f)
                        .withClientCurrency("ZWD"),

                500, -32603,
                "Currency from balance ZWD doesn't match client currency RUB for ClientID"
        );
        ClientInfo clientInfo = api.userSteps.clientSteps().getClientInfo(rubLogin);
        assertThat("Значение overdraftSumAvailable изменилось", clientInfo.getOverdraftSumAvailableInCurrency(),
                equalTo(0.0f));

        ClientsOptionsRecord clientsOptions = api.userSteps.getDirectJooqDbSteps().clientsOptionsSteps().getClientOptions(rubClientID);
        assertThat("В таблице ClientsOptions не должно быть записи для клиента", clientsOptions, nullValue());
    }

}
