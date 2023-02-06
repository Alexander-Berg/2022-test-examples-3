package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
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

@Aqua.Test(title = "NotifyOrder2 - оплата кампании")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2Test {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2Test.class);
    private long campaignId;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB)
            .wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Before
    public void init() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
    }


    @Test
    public void notifyOrder2Test() {
        Money qty = Money.valueOf(100.0f, Currency.RUB);
        log.info("Вызываем метод NotifyOrder2. Оплачиваем кампанию " + campaignId);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(qty.floatValue())
                .withProductCurrency(Currency.RUB.value())
        );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(),
                equalTo(qty.bidLong().longValue()));
    }

    @Test
    public void notifyOrder2WithZeroQtyTest() {
        log.info("Вызываем метод NotifyOrder2 для валютного клиента. Оплачиваем кампанию " + campaignId
                + ", сумма = 0");
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(Money.valueOf(100.0f, Currency.RUB).addVAT().floatValue())
                .withProductCurrency(Currency.RUB.value())
        );
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(0f)
                .withProductCurrency(Currency.RUB.value())
        );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));
    }
}
