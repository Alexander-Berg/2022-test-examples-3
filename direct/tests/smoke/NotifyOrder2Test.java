package ru.yandex.autotests.directintapi.tests.smoke;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignFundsEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by omaz on 11.04.14.
 */
@Aqua.Test
@Features(FeatureNames.BALANCE_INTAPI_MONITOR)
public class NotifyOrder2Test {

    protected LogSteps log = LogSteps.getLogger(this.getClass());

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public BottleMessageRule bmr = new BottleMessageRule();

    @Stories("NotifyOrder2. Получение.")
    @Test
    public void notifyOrder2CurrencyTest() {
        api.as(Logins.LOGIN_RUB);
        Money qty = Money.valueOf(100.0f, Currency.RUB);
        Double clientVatRate = api.userSteps.clientsStepsV5().getClientVatRate(Logins.LOGIN_RUB);
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        log.info("Вызываем метод NotifyOrder2 для рублевого клиента. Оплачиваем кампанию " + campaignId);
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(campaignId)
                        .withTimestamp()
                        .withConsumeQty(qty.floatValue())
                        .withProductCurrency(Currency.RUB.value())
                );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assumeThat(
                "У клиента не должнен быть включен общий счет",
                campaignGetItem.getFunds().getMode(),
                equalTo(CampaignFundsEnum.CAMPAIGN_FUNDS));

        assertThat("Не принимаются нотификации Баланса на валютные кампании",
                campaignGetItem.getFunds().getCampaignFunds().getSum(),
                equalTo(qty.divideValueUsingCustomVatRate(clientVatRate).bidLong().longValue()));
    }
}
