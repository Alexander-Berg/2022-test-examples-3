package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by pavryabov on 14.05.15.
 * https://st.yandex-team.ru/TESTIRT-5459
 */
@Aqua.Test()
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Description("Проверка правильности обработки флага force_currency_convert_accepted_at из нотификации NotifyClient2")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class NotifyClient2CanBeForceCurrencyConvertedTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();
    private static String manager = Logins.LOGIN_MNGR;
    private String login;
    private Long clientID;
    private static final String ACCEPTED_AT = "force_currency_convert_accepted_at";

    @Step("Подготовка данных для теста")
    @Before
    public void createClient() {
        api.as(manager);
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        CreateNewSubclientResponse createNewSubclientResponse =
                clientStepsHelper.createServicedClient("intapi-servClient-", Logins.LOGIN_MNGR);
        login = createNewSubclientResponse.getLogin();
        clientID = Long.valueOf(createNewSubclientResponse.getClientID());
        assumeThat("force_currency_convert_accepted_at не установлен",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, ACCEPTED_AT)
                        .getForceCurrencyConvertAcceptedAt(), nullValue());
    }

    @Test
    public void forceCurrencyConvertAcceptedAtOn() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest().defaultParams()
                        .withClientID(clientID)
                        .withCanBeForceCurrencyConverted(true)
        );
        assertThat("force_currency_convert_accepted_at установлен",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, ACCEPTED_AT)
                        .getForceCurrencyConvertAcceptedAt(),
                notNullValue());
    }

    @Test
    public void forceCurrencyConvertAcceptedAtOff() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest().defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withCanBeForceCurrencyConverted(false)
        );
        assertThat("force_currency_convert_accepted_at не установлен",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, ACCEPTED_AT)
                        .getForceCurrencyConvertAcceptedAt(),
                nullValue());
    }
}
