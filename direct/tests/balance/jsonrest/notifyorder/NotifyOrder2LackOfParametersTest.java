package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: pavryabov
 * Date: 19.03.13
 * https://jira.yandex-team.ru/browse/TESTIRT-340
 */
@Aqua.Test(title = "NotifyOrder2 - недостаточный набор параметров")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2LackOfParametersTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2LackOfParametersTest.class);

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB)
            .wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Test
    public void notifyOrder2WithEmptyParamsTest() {
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(new NotifyOrder2JSONRequest(),
                400, 1008, "invalid service ");
    }

    @Test
    public void notifyOrder2WithLackOfServiceIDTest() {
        log.info("Вызываем метод NotifyOrder2 без ServiceID");
        long campaignId = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
        float qty = 100.0f;
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withConsumeQty(qty)
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1008, "invalid service "
                );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании не изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));

    }

    @Test
    public void notifyOrder2WithLackOfSCampaignIDTest() {
        float qty = 100.0f;
        log.info("Вызываем метод NotifyOrder2 без CampaignID");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withTimestamp()
                                .withConsumeQty(qty)
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1016, "Invalid ServiceOrderID from balance: null"
                );
    }

    @Test
    public void notifyOrder2WithNonexistCampaignIDTest() {
        float qty = 100.0f;
        log.info("Вызываем метод NotifyOrder2 для несуществующей кампании");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(0l)
                                .withTimestamp()
                                .withConsumeQty(qty)
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1016, "Invalid ServiceOrderID from balance: 0"
                );
    }

    @Test
    public void notifyOrder2WithLackOfTimestampTest() {
        long campaignId = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
        float qty = 100.0f;
        log.info("Вызываем метод NotifyOrder2 без таймстампа");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withConsumeQty(qty)
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1013, "No valid Tid given"
                );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании не изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));
    }

    @Test
    public void notifyOrder2WithConsumeMoneyQtyInsteadOfQtyTest() {
        long campaignId = api.as(Logins.LOGIN_RUB).userSteps.campaignSteps().addDefaultTextCampaign();
        float qty = 100.0f;
        log.info("Вызываем метод NotifyOrder2 для обычного клиента с Amount вместо Qty");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withConsumeMoneyQty(qty)
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1009, "invalid ConsumeQty from balance: undef, must be >= 0"
                );

        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));

    }

    @Test
    public void notifyOrder2WithLackOfAmountTest() {
        long campaignId = api.as(Logins.LOGIN_RUB).userSteps.campaignSteps().addDefaultTextCampaign();
        log.info("Вызываем метод NotifyOrder2 для рублевого клиента без Amount");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1009, "invalid ConsumeQty from balance: undef, must be >= 0"
                );

        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании не изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));
    }


    @Test
    public void notifyOrder2WithLackOfCurrencyTest() {
        long campaignId = api.as(Logins.LOGIN_RUB).userSteps.campaignSteps().addDefaultTextCampaign();
        float qty = 100.0f;
        log.info("Вызываем метод NotifyOrder2 для рублевого клиента без ProductCurrency");
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withConsumeQty(qty),
                        500, -32603, "No ProductCurrency given for Direct campaign"
                );

        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));
    }

}
