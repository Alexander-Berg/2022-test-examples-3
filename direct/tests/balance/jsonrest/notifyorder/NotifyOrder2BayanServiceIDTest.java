package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: xy6er
 * https://st.yandex-team.ru/TESTIRT-8717
 * https://st.yandex-team.ru/TESTIRT-3618
 */
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-38456"),
        @Issue("https://st.yandex-team.ru/DIRECT-35285")
})
@Aqua.Test(title = "NotifyOrder2 - Bayan ServiceID")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@RunWith(Parameterized.class)
public class NotifyOrder2BayanServiceIDTest {

    private static long campaignId;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_YNDX_FIXED);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter
    public String productCurrency;

    @Parameterized.Parameters(name = "ProductCurrency = {0}")
    public static Collection testData() {
        return Arrays.asList(new Object[][]{
                //DIRECT-35285#1458573019000: По баяновским кампаниям нотификации по-прежнему приезжают без ProductCurrency, их должны по-прежнему продолжить принимать
                {null},
                {Currency.YND_FIXED.value()}
        });
    }

    @BeforeClass
    public static void init() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setType(campaignId, CampaignsType.MCB);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_YNDX_FIXED);
    }

    @Test
    @Description("Вызываем NotifyOrder2 для МКБ кампании. Проверяем, что сумма у кампании изменилась")
    public void notifyOrder2WithBayanServiceIDAndProductCurrencyTest() {
        AllureUtils.changeTestCaseTitle("Вызов NotifyOrder2 c BAYAN_SERVICE_ID и ProductCurrency = " + productCurrency);
        float qty = 100.0f;
        NotifyOrder2JSONRequest request = new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.BAYAN_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(qty);
        if (productCurrency != null) {
            request.withProductCurrency(productCurrency);
        }
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(request);
        CampaignsRecord campaigns = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании изменился", campaigns.getSum(), equalTo(BigDecimal.valueOf(qty).setScale(6)));
    }
}
