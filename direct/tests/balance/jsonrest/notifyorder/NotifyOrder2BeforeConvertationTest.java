package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsMulticurrencySumsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
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
 * Author: xy6er
 * https://jira.yandex-team.ru/browse/DIRECT-25033
 */
@Aqua.Test(title = "NotifyOrder2 - нотификации перед конвертацией клиента")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2BeforeConvertationTest {
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2ConvertationLockTest.class);
    private long campaignId;
    private String login;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        log.info("Создаем фишкового клиента (сервисируемого)");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient12-", Logins.LOGIN_MNGR);
        login = clientInfo.getLogin();
        Integer clientId = clientInfo.getClientID();
        DirectJooqDbSteps jooqDbSteps = api.userSteps.getDirectJooqDbSteps();
        jooqDbSteps.useShardForLogin(login);
        jooqDbSteps.clientsOptionsSteps().setClientFlag(clientId.longValue(), CREATE_WITHOUT_WALLET);
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(login);
    }


    @Test
    public void notifyOrder2BeforeConvertationTest() {
        float qty = 5;              //текущая сумма в фишках
        float moneyQty = 100;       //общая сумма денег на кампании в будущей валюте клиента
        float fixedMoneyQty = 50;   //стоимость в деньгах потраченных на кампании фишек, в будущей валюте кампании

        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), ConvertType.MODIFY,
                ConvertationLockSteps.LOCK_DELAY_MINUTES + 1);
        log.info("Вызываем метод NotifyOrder2. Оплачиваем кампанию " + campaignId);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(qty)
                .withConsumeMoneyQty(moneyQty)
                .withCompletionFixedMoneyQty(fixedMoneyQty)
                .withProductCurrency(Currency.YND_FIXED.value())
        );

        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(login, campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(
                Money.valueOf(qty).bidLong().longValue()));
        CampaignsMulticurrencySumsRecord sumsRecord =
                api.userSteps.getDirectJooqDbSteps().multicurrencySteps().getCampSumsForCid(campaignId);
        assertThat("ConsumeMoneyQty кампании изменился",
                sumsRecord.getSum().floatValue(), equalTo(moneyQty));
        assertThat("CompletionFixedMoneyQty кампании изменился",
                sumsRecord.getChipsCost().floatValue(), equalTo(fixedMoneyQty));
    }
}


