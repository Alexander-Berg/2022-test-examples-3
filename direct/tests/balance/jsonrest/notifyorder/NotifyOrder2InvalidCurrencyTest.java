package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.util.Arrays;
import java.util.Collection;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
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
 * User: omaz, xy6er
 * Date: 19.03.13
 * https://jira.yandex-team.ru/browse/TESTIRT-340
 */

@Aqua.Test(title = "NotifyOrder2 - невалидная валюта")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@RunWith(Parameterized.class)
public class NotifyOrder2InvalidCurrencyTest {
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2InvalidCurrencyTest.class);
    private static long campaignId;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter
    public String currency;

    @Parameterized.Parameters(name = "Currency = {0}")
    public static Collection parameterizedData() {
        return Arrays.asList(new Object[][]{
                {Currency.USD.value()},
                {Currency.YND_FIXED.value()},
                {"ZWD"},
                {""},
        });
    }

    @BeforeClass
    public static void init() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.LOGIN_RUB);
    }


    @Test
    public void notifyOrder2WithInvalidCurrencyTest() {
        float qty = 100.0f;
        String errorText = "Our product currency RUB doesn't match Balance product currency ";
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withConsumeQty(qty)
                                .withProductCurrency(currency),
                        500, -32603, errorText + currency
                );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(Logins.LOGIN_RUB, campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании не изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));
    }

}
