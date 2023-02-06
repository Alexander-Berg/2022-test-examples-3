package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BsResyncQueueRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 14.07.16.
 * https://st.yandex-team.ru/TESTIRT-9669
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Description("Проверка переотправки кампаний клиента при смене флага is_business_unit")
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-54147"), @Issue("https://st.yandex-team.ru/DIRECT-65455")})
@RunWith(Parameterized.class)
public class NotifyClient2BusinessUnitChangeAndResyncTest {

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);
    @Rule
    public Trashman trashman = new Trashman(api);

    @Parameterized.Parameter(0)
    public String login;

    @Parameterized.Parameter(1)
    public Currency balanceCurrency;

    @Parameterized.Parameter(2)
    public Integer initialIsBU;

    @Parameterized.Parameter(3)
    public Boolean balanceIsBu;

    @Parameterized.Parameter(4)
    public Matcher matcher;

    @Parameterized.Parameter(5)
    public Boolean rmClientsOptionsRecord;

    private Long clientID;

    private Long cid;

    @Parameterized.Parameters(name = "Исходное состояние-{2}, что приходит в NotifyClient2-{3}, надо ли удалять запись в clientsOptions-{5}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {Logins.BUSINESS_UNIT2, Currency.RUB, 1, false, iterableWithSize(1), false},
                {Logins.BUSINESS_UNIT2, Currency.RUB, 0, true, iterableWithSize(1), false},
                {Logins.BUSINESS_UNIT2, Currency.RUB, 0, false, iterableWithSize(0), false},
                {Logins.BUSINESS_UNIT2, Currency.RUB, 1, true, iterableWithSize(0), false},
                {Logins.BUSINESS_UNIT2, Currency.RUB, 1, null, iterableWithSize(0), false},

                //Для пользователей без записи в clientsOptions выставлять initialBu нет смысла, поэтому для них initialBu = null
                {Logins.NO_CLIENTSOPTIONS_ENTRY_USER, Currency.RUB, null, true, iterableWithSize(1), true},
                {Logins.NO_CLIENTSOPTIONS_ENTRY_USER, Currency.RUB, null, false, iterableWithSize(0), true},
        });
    }


    @Before
    @Step("подготовка тестовых данных")
    public void prepare() {
        api = api.as(login);
        clientID = Long.valueOf(User.get(login).getClientID());
        int shard = api.userSteps.clientFakeSteps().getUserShard(login);
        api.userSteps.getDirectJooqDbSteps().useShard(shard);
        if (initialIsBU != null) {
            api.userSteps.getDirectJooqDbSteps().clientsOptionsSteps()
                    .setClientOptionsIsBusinessUnit(clientID, initialIsBU);
        }
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignFakeSteps().setBSSynced(cid, true);
        api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
        if (rmClientsOptionsRecord) {
            api.userSteps.getDirectJooqDbSteps().clientsOptionsSteps().dropClientRecord(clientID);
        }
    }

    @Test
    public void notifyClientWithBusinessUnitResyncQueueTest() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyClientJsonSteps().notifyClientNoErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withClientCurrency(balanceCurrency.value())
                        .withOverdraftLimit(1000f)
                        .withOverdraftSpent(10f)
                        .withBusinessUnit(balanceIsBu)
        );

        List<BsResyncQueueRecord> bsResyncQueueList = api.userSteps.getDirectJooqDbSteps().bsResyncQueueSteps().getBsResyncQueueRecordsByCid(cid);
        assertThat("верный список кампаний на переотправку", bsResyncQueueList,
                matcher);
    }
}
