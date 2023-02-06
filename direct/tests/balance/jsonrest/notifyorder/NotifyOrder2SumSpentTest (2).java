package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsCurrency;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: xy6er
 * https://st.yandex-team.ru/TESTIRT-4001
 */

@Aqua.Test(title = "NotifyOrder2 - Sum_spent")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2SumSpentTest {

    /**
     * ProductId для баяновского продукта.
     * В директе цена продукта соответствует 1000 показов
     */
    private static final long BAYAN_PRODUCT_ID = 2584;
    private static float bayanProductPrice;
    private static DarkSideSteps darkSideSteps;
    private static long bayanCampaignID;
    private static long directCampaignID;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @BeforeClass
    public static void init() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        bayanCampaignID = api.userSteps.campaignSteps().addDefaultTextCampaign();
        CampaignsRecord campaigns =
                api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN).campaignsSteps()
                        .getCampaignById(bayanCampaignID);
        campaigns.setType(ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsType.mcb)
                .setCurrency(CampaignsCurrency.YND_FIXED);

        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN).campaignsSteps()
                .updateCampaigns(campaigns);
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(bayanCampaignID);
        campaignFakeInfo.setProductID((int) BAYAN_PRODUCT_ID);
        darkSideSteps.getCampaignFakeSteps().fakeCampaignParams(campaignFakeInfo);

        directCampaignID = api.userSteps.campaignSteps().addDefaultTextCampaign();

        bayanProductPrice = api.userSteps.getDirectJooqDbSteps().productsSteps().getProductPrice(BAYAN_PRODUCT_ID).floatValue();
    }

    @AfterClass
    public static void deleteCampaign() {
        //Нужно для успешной удалении кампании Trashman-ом
        darkSideSteps.getCampaignFakeSteps().setType(bayanCampaignID, CampaignsType.TEXT);
    }

    @Test
    public void sumSpentNotChangeAfterCallNotifyOrder2ForBayanCampaign() {
        int sumUnits = 900;
        int sumSpentUnits = 300;
        float sumSpent = 150.0f;
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.BAYAN_SERVICE_ID)
                .withServiceOrderId(bayanCampaignID)
                .withTimestamp()
                .withConsumeQty((float) sumUnits)
        );
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(bayanCampaignID);
        campaignFakeInfo.setSumSpentUnits(sumSpentUnits);
        campaignFakeInfo.setSumSpent(sumSpent);
        darkSideSteps.getCampaignFakeSteps().fakeCampaignParams(campaignFakeInfo);

        sumUnits += 400;
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.BAYAN_SERVICE_ID)
                .withServiceOrderId(bayanCampaignID)
                .withTimestamp()
                .withConsumeQty((float) sumUnits)
        );

        campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(bayanCampaignID);
        float sum = bayanProductPrice * sumUnits / 1000f;
        float expectedSumSpent = sum * sumSpentUnits / sumUnits;
        assertThat("sum_spent кампании не изменился",
                campaignFakeInfo.getSumSpent(),
                equalTo(BigDecimal.valueOf(expectedSumSpent).setScale(6, RoundingMode.HALF_UP).floatValue())
        );
    }

    @Test
    public void sumSpentNotChangeAfterCallNotifyOrder2ForDirectCampaign() {
        float qty = 150.0f;
        float sumSpent = 100.0f;
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(directCampaignID)
                .withTimestamp()
                .withConsumeQty(qty)
                .withProductCurrency(Currency.RUB.value())
        );
        darkSideSteps.getCampaignFakeSteps().setSumSpent(directCampaignID, sumSpent);

        qty += 400;
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(directCampaignID)
                .withTimestamp()
                .withConsumeQty(qty)
                .withProductCurrency(Currency.RUB.value())
        );

        CampaignFakeInfo campaignFakeInfo =
                darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(directCampaignID);
        assertThat("sum_spent кампании не должен был измениться", campaignFakeInfo.getSumSpent(), equalTo(sumSpent));
    }

}
