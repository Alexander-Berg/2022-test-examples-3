package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.common.Value;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test()
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Description("Проверка правильности обработки флага нерезидента из нотификации NotifyClient2")
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-34588"),
        @Issue("https://st.yandex-team.ru/DIRECT-37012")
})
public class NotifyClient2NotResidentTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static String agencyLogin = ru.yandex.autotests.directapi.model.Logins.AGENCY_RUB;
    private static String login;
    private static Long clientID;
    private static final String NOT_RESIDENT = "non_resident_client";

    @BeforeClass
    public static void getClientID() {
        api.userSteps.clientFakeSteps().enableToCreateSubClients(agencyLogin);
        CreateNewSubclientResponse createNewSubclientResponse = api.as(agencyLogin).userSteps.clientSteps()
                .createNewAgencySubClient("subclient-", agencyLogin, Currency.RUB);
        login = createNewSubclientResponse.getLogin();
        clientID = (long) createNewSubclientResponse.getClientID();
    }

    @Test
    public void notResidentInNotifyClientOn() {
        darkSideSteps.getClientFakeSteps().setNonResidentClient(login, Value.NO);
        assumeThat("флаг нерезидента у клиента выключен",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, NOT_RESIDENT).getNonResidentClient(),
                equalTo(Value.NO));
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withClientCurrency(Currency.RUB.toString())
                        .withNonResident(true)
        );
        assertThat("клиент стал нерезидентом",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, NOT_RESIDENT).getNonResidentClient(),
                equalTo(Value.YES));
    }

    @Test
    public void notResidentInNotifyClientOff() {
        darkSideSteps.getClientFakeSteps().setNonResidentClient(login, Value.YES);
        assumeThat("флаг нерезидента у клиента включен",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, NOT_RESIDENT).getNonResidentClient(),
                equalTo(Value.YES));
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withClientCurrency(Currency.RUB.toString())
                        .withNonResident(false)
        );
        assertThat("клиент перестал быть нерезидентом",
                darkSideSteps.getClientFakeSteps().getClientDataByLogin(login, NOT_RESIDENT).getNonResidentClient(),
                equalTo(Value.NO));
    }
}
