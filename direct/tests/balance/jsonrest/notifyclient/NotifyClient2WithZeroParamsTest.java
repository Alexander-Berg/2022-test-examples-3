package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ClientsOptionsStatusbalancebanned;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsOptionsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.math.BigDecimal;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - Пустая нотификация")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2WithZeroParamsTest {

    private static final float ZERO_SUM = 0.0f;
    private static final String ZERO_DATE = "0000-00-00";

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    private Long clientID;
    private String login = Logins.LOGIN_RUB;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void beforeTests() {
        clientID = Long.parseLong(darkSideSteps.getClientFakeSteps().getClientData(login).getClientID());
    }


    @Test
    public void notifyClientWithZeroParamsTest() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(ZERO_SUM)
                        .withOverdraftSpent(ZERO_SUM)
                        .withMinPaymentTerm(ZERO_DATE)
                        .withClientCurrency("")
        );

        ClientsOptionsRecord actualClientOptions = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .clientsOptionsSteps().getClientOptions(clientID);

        ClientsOptionsRecord expectedClientsOptions = new ClientsOptionsRecord()
                .setClientid(clientID)
                .setOverdraftLim(BigDecimal.valueOf(ZERO_SUM).setScale(2))
                .setDebt(BigDecimal.valueOf(ZERO_SUM).setScale(2))
                .setStatusbalancebanned(ClientsOptionsStatusbalancebanned.No);
        /**
         * xy6er: почему в базе для поля NextPayDate сохраняется значение «0001-01-01» когда мы передаем «0000-00-00»?
         * pankovpv: часовые пояса и таймзоны это :) пусть живёт как есть)
         */
        //expectedClientsOptions.setNextPayDate(ZERO_SUM);
        assertThat("Значения в таблице ClientsOptions соответствуют ожидаемому",
                actualClientOptions.intoMap(), beanDiffer(expectedClientsOptions.intoMap())
                        .useCompareStrategy(DefaultCompareStrategies
                                .onlyExpectedFields())
        );
    }
}
