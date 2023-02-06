package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsOptionsRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 14.07.16
 * https://st.yandex-team.ru/TESTIRT-9669
 */
@Aqua.Test(title = "NotifyClient2 - обновление BusinessUnit")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-54147")
@RunWith(Parameterized.class)
public class NotifyClient2BusinessUnitTest {
    private static final String LOGIN = Logins.BUSINESS_UNIT3;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static Long clientID;
    private static int shard;

    @Parameterized.Parameter(0)
    public int initialIsBU;

    @Parameterized.Parameter(1)
    public Boolean fromBalance;

    @Parameterized.Parameter(2)
    public int expectedIsBU;

    @Parameterized.Parameters(name = "Исходное состояние-{0}, что приходит в NotifyClient2-{1}, " +
            "что ждем в результате - {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {1, false, 0},
                {1, true, 1},
                {0, true, 1},
                {0, false, 0},
                {1, null, 1},
                {0, null, 0},
        });
    }

    @BeforeClass
    public static void before() {
        clientID = Long.valueOf(User.get(LOGIN).getClientID());
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
    }

    @Before
    @Step("подготовка тестовых данных")
    public void prepare() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).clientsOptionsSteps()
                .setClientOptionsIsBusinessUnit(clientID, initialIsBU);
    }

    @Test
    public void notifyClientWithBusinessUnitTest() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest().defaultParams()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withClientCurrency(Currency.RUB.value())
                        .withOverdraftLimit(1000f)
                        .withOverdraftSpent(10f)
                        .withBusinessUnit(fromBalance)
        );
        ClientsOptionsRecord clientOptions
                = api.userSteps.getDirectJooqDbSteps().useShard(shard).clientsOptionsSteps().getClientOptions(clientID);
        assertThat("в базе верное значение ppc.client_options.is_business_unit", clientOptions.getIsBusinessUnit(),
                equalTo(expectedIsBU));
    }

}
