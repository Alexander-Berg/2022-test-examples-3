package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsOptionsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - изменение задолженности")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2ChangeDebtTest {

    protected static LogSteps log = LogSteps.getLogger(NotifyClient2ChangeDebtTest.class);
    private Long clientID;
    private String login;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class).as(Logins.LOGIN_SUPER);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void before() {
        log.info("Создаем клиента для теста");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient3-", Logins.LOGIN_MNGR);
        clientID = (long) clientInfo.getClientID();
        login = clientInfo.getLogin();
    }


    @Test
    public void notifyClientChangeDebtTest() {
        log.info("Вызываем метод NotifyClient. Проверяем изменение задолженности.");
        float withDebt = 100.0f;
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(withDebt)
        );
        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставилось верное значение debt",
                clientOptions.getDebt(), equalTo(BigDecimal.valueOf(withDebt).setScale(2)));
    }

    @Test
    public void notifyClientChangeDebtToZeroTest() {
        log.info("Вызываем метод NotifyClient. Проверяем изменение задолженности на 0.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(100.0f)
        );

        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0f)
        );
        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставилось верное значение debt",
                clientOptions.getDebt(), equalTo(BigDecimal.valueOf(0f).setScale(2)));
    }
}
