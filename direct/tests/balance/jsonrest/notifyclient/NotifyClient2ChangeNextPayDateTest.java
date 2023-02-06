package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.joda.time.DateTime;
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
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - изменение даты платежа")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2ChangeNextPayDateTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyClient2ChangeNextPayDateTest.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private Long clientID;
    private String login;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void before() {
        log.info("Создаем клиента для теста");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient-", Logins.LOGIN_MNGR);
        clientID = (long) clientInfo.getClientID();
        login = clientInfo.getLogin();
    }

    @Test
    public void notifyClientChangeNextPayDateTest() {
        Date date = DateTime.now().plusDays(1).withTimeAtStartOfDay().toDate();
        log.info("Вызываем метод NotifyClient. Проверяем изменение даты платежа.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0.1f)
                        .withMinPaymentTerm(sdf.format(date))
        );
        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставилось верное значение nextPayDate",
                clientOptions.getNextpaydate().getTime(), equalTo(date.getTime()));
    }

    @Test
    public void notifyClientChangeNextPayDateToOldTest() {
        Date date = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
        log.info("Вызываем метод NotifyClient. Проверяем изменение даты платежа на просроченную.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(100f)
                        .withMinPaymentTerm(sdf.format(date))
        );
        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставилось верное значение nextPayDate",
                clientOptions.getNextpaydate().getTime(), equalTo(date.getTime()));
        //TODO что ещё должно происходить при просроченном платеже?
    }
}
