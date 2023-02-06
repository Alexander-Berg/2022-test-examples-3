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
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - одновременное изменение задолженности и лимита овердрафта")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2ChangeOverdraftAndDebtTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyClient2ChangeOverdraftAndDebtTest.class);
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
                .createServicedClient("intapi-servClient2-", Logins.LOGIN_MNGR);
        clientID = (long) clientInfo.getClientID();
        login = clientInfo.getLogin();
    }

    @Test
    public void notifyClientChangeDebtLessThanOverdraftTest() {
        log.info("Вызываем метод NotifyClient. Проверяем изменение задолженности и лимита овердрафта. Задолженность меньше лимита.");
        float overdraftLimit = 150.0f;
        float withDebt = 100.0f;
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit)
                        .withOverdraftSpent(withDebt)
        );
        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставились верные значения debt и overdraft_lim",
                clientOptions.intoMap(),
                beanDiffer(new ClientsOptionsRecord()
                        .setDebt(BigDecimal.valueOf(withDebt).setScale(2))
                        .setOverdraftLim(BigDecimal.valueOf(overdraftLimit).setScale(2))
                        .intoMap()
                ).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void notifyClientChangeDebtEqualToOverdraftTest() {
        float overdraftLimit = 100.0f;
        float withDebt = 100.0f;
        log.info("Вызываем метод NotifyClient. Проверяем изменение задолженности и лимита овердрафта. Задолженность равна лимиту.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit)
                        .withOverdraftSpent(withDebt)
        );

        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставились верные значения debt и overdraft_lim",
                clientOptions.intoMap(),
                beanDiffer(new ClientsOptionsRecord()
                        .setDebt(BigDecimal.valueOf(withDebt).setScale(2))
                        .setOverdraftLim(BigDecimal.valueOf(overdraftLimit).setScale(2))
                        .intoMap()
                ).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void notifyClientChangeDebtMoreThanOverdraftTest() {
        float overdraftLimit = 100.0f;
        float withDebt = 150.0f;
        log.info("Вызываем метод NotifyClient. Проверяем изменение задолженности и лимита овердрафта. Задолженность больше лимита.");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit)
                        .withOverdraftSpent(withDebt)
        );
        ClientsOptionsRecord clientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе выставились верные значения debt и overdraft_lim",
                clientOptions.intoMap(),
                beanDiffer(new ClientsOptionsRecord()
                        .setDebt(BigDecimal.valueOf(withDebt).setScale(2))
                        .setOverdraftLim(BigDecimal.valueOf(overdraftLimit).setScale(2))
                        .intoMap()
                ).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
