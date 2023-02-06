package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.util.Arrays;
import java.util.Collection;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.ConvertationLockSteps;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.clients.ConvertType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.db.steps.ClientsOptionsSteps.ClientFlagsEnum.CREATE_WITHOUT_WALLET;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: omaz
 * Date: 14.11.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1125
 */

@Aqua.Test(title = "NotifyOrder2 - блокировка при конвертации клиента")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@RunWith(Parameterized.class)
public class NotifyOrder2ConvertationLockTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2ConvertationLockTest.class);
    private long campaignId;
    private CreateNewSubclientResponse clientInfo;
    private String login;

    @Parameterized.Parameter
    public String convertType;

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameters(name = "Тип конвертации: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {ConvertType.COPY},
                {ConvertType.MODIFY}};
        return Arrays.asList(data);
    }

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        log.info("Создаем фишкового клиента (сервисируемого)");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        clientInfo = clientStepsHelper.createServicedClient("intapi-servClient13-", Logins.LOGIN_MNGR);
        login = clientInfo.getLogin();
        Integer clientId = clientInfo.getClientID();
        DirectJooqDbSteps jooqDbSteps = api.userSteps.getDirectJooqDbSteps();
        jooqDbSteps.useShardForLogin(login);
        jooqDbSteps.clientsOptionsSteps().setClientFlag(clientId.longValue(), CREATE_WITHOUT_WALLET);
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(login);
    }


    @Test
    public void notifyOrder2ConvertationMoreThan15MinutesTest() {
        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), convertType,
                ConvertationLockSteps.LOCK_DELAY_MINUTES + 1);

        log.info("Вызываем метод NotifyOrder2. Проверяем, что вызов проходит, если время до конвертации > 15 минут");
        Money qty = Money.valueOf(100.0f);
        log.info("Вызываем метод NotifyOrder2. Оплачиваем кампанию " + campaignId);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(qty.floatValue())
                .withProductCurrency(Currency.YND_FIXED.value())
        );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(login, campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(),
                equalTo(qty.bidLong().longValue()));
    }

    @Test
    public void notifyOrder2ConvertationLessThan15MinutesTest() {
        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), convertType,
                ConvertationLockSteps.LOCK_DELAY_MINUTES - 1);

        log.info("Вызываем метод NotifyOrder2. Проверяем, что вызов не проходит, если время до конвертации < 15 минут");
        float qty = 100.0f;
        log.info("Вызываем метод NotifyOrder2. Оплачиваем кампанию " + campaignId);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(campaignId)
                        .withTimestamp()
                        .withConsumeQty(qty)
                        .withProductCurrency(Currency.YND_FIXED.value()),
                423, -32603, "Client " + clientInfo.getClientID() +
                        " is going to currency convert soon, will accept notifications after it's done"
        );
        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Баланс кампании не изменился", campaignFakeInfo.getSum(), equalTo(0.0f));
    }
}
