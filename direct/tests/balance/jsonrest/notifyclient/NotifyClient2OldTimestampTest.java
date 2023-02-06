package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

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
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - старый таймстамп")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
public class NotifyClient2OldTimestampTest {
    protected static LogSteps log = LogSteps.getLogger(NotifyClient2OldTimestampTest.class);
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
                .createServicedClient("intapi-servClient8-", Logins.LOGIN_MNGR);
        clientID = (long) clientInfo.getClientID();
        login = clientInfo.getLogin();
    }


    @Test
    public void notifyClientWithOldTimestampTest() {
        //В этом месте Баланс может сделать вызов NotifyClient2 для clientID, что приведет к установке tid
        //Long.MAX_VALUE используется, чтобы гарантированно выиграть у Баланса и обновить tid(и запись в clients_options)
        String tidString = Long.toString(Long.MAX_VALUE);
        log.info("Вызываем метод NotifyClient один раз, чтобы записать таймстамп.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTid(tidString)
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0f)
                        .withMinPaymentTerm("2013-09-01")
        );
        ClientsOptionsRecord initialClientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        log.info("Вызываем метод NotifyClient со старым timestamp'ом.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTid(tidString)
                        .withOverdraftLimit(0f)
                        .withOverdraftSpent(0.01f)
                        .withMinPaymentTerm("2013-09-01")
        );

        ClientsOptionsRecord actualClientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("запись в базе не обновилась",
                actualClientOptions.intoMap(), beanDiffer(initialClientOptions.intoMap())
                        .useCompareStrategy(DefaultCompareStrategies
                                .allFieldsExcept(BeanFieldPath.newPath("nextPayDate")))
        );
        //beanDiffer не умеет java.sql.Date :(
        assertThat("в базе выставилось верное значение nextPayDate",
                actualClientOptions.getNextpaydate().getTime(),
                equalTo(initialClientOptions.getNextpaydate().getTime()));

    }
}
