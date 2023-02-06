package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletCampaignsRecord;
import ru.yandex.autotests.direct.db.steps.WalletCampaignsSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.tests.IntapiConstants;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pashkus on 26.04.16.
 * https://st.yandex-team.ru/TESTIRT-9023
 */
@Issue("https://st.yandex-team.ru/DIRECT-52051")
@Aqua.Test
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2TotalConsumeQtyForRubWalletTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    public static BigDecimal TOTAL_CONSUME_QTY = BigDecimal.valueOf(300);
    private static int shard;
    public final Float QTY = 100f;
    public final String CURRENCY = Currency.RUB.value();

    public static Long cid;
    public static Long walletCid;


    /**
     * @ Создаем и кампанию с общим счетом в рублях
     */
    @BeforeClass
    public static void prepareTestData() {
        String login = Logins.LOGIN_WALLET;
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign(login);
        walletCid = (long) api.userSteps.campaignFakeSteps().fakeGetCampaignParams(cid).getWalletCid();
        shard = api.userSteps.clientFakeSteps().getUserShard(login);
    }

    @Step("Обнуляем значение wallet_campaigns.total_sum")
    @Before
    public void zeroTotalSum() {
        WalletCampaignsSteps walletCampaignsSteps = api.userSteps.getDirectJooqDbSteps().useShard(shard).walletCampaignsSteps();
        WalletCampaignsRecord walletCampaign = walletCampaignsSteps.getWalletCampaigns(walletCid);
        walletCampaign.setTotalSum(BigDecimal.ZERO);
        walletCampaignsSteps.updateWalletCampaigns(walletCampaign);
    }

    @Test
    @Description("(1.2) NotifyOrder2 без TotalConsumeQty для кошелька" +
            "Ожидается, что получим ошибку: " + IntapiConstants.NOTIFY_ORDER_ERROR_1)
    public void notifyOrder2WithoutTotalConsumeQtyForWallet() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(walletCid)
                                .withTimestamp()
                                .withConsumeQty(QTY)
                                .withProductCurrency(CURRENCY),
                        400, 1015, IntapiConstants.NOTIFY_ORDER_ERROR_1
                );

        WalletCampaignsRecord walletCampaign =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).walletCampaignsSteps()
                        .getWalletCampaigns(walletCid);
        assertThat("total_sum кошелька не изменился",
                walletCampaign.getTotalSum(), equalTo(BigDecimal.ZERO.setScale(6)));
    }

    @Test
    @Description("(1.4) NotifyOrder2 без TotalConsumeQty для кампании под кошельком" +
            "Ожидается, что поле проигнорируется; изменений в total_sum быть не должно")
    public void notifyOrder2WithoutTotalConsumeQtyForCampUnderWallet() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cid)
                        .withTimestamp()
                        .withConsumeQty(QTY)
                        .withProductCurrency(CURRENCY)
                );
        WalletCampaignsRecord walletCampaign =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).walletCampaignsSteps()
                        .getWalletCampaigns(walletCid);
        assertThat("total_sum кошелька не изменился",
                walletCampaign.getTotalSum(), equalTo(BigDecimal.ZERO.setScale(6)));
    }

    @Test
    @Description("(2.2) NotifyOrder2 с TotalConsumeQty для кошелька" +
            "Ожидается, что TotalConsumeQty сохранилось в таблице wallet_campaigns в поле total_sum")
    public void notifyOrder2WithTotalConsumeQtyForWallet() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(walletCid)
                        .withTimestamp()
                        .withConsumeQty(QTY)
                        .withTotalConsumeQty(TOTAL_CONSUME_QTY.floatValue())
                        .withProductCurrency(CURRENCY)
                );
        WalletCampaignsRecord walletCampaign =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).walletCampaignsSteps()
                        .getWalletCampaigns(walletCid);
        assertThat("total_sum кошелька изменился", walletCampaign.getTotalSum(), equalTo(TOTAL_CONSUME_QTY.setScale(6)));
    }

    @Test
    @Description("(2.4) NotifyOrder2 с TotalConsumeQty для кампании под кошельком" +
            "Ожидается, что поле проигнорируется; изменений в total_sum быть не должно")
    public void notifyOrder2WithTotalConsumeQtyForCampUnderWallet() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cid)
                        .withTimestamp()
                        .withConsumeQty(QTY)
                        .withTotalConsumeQty(TOTAL_CONSUME_QTY.floatValue())
                        .withProductCurrency(CURRENCY)
                );
        WalletCampaignsRecord walletCampaign =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).walletCampaignsSteps()
                        .getWalletCampaigns(walletCid);
        assertThat("total_sum кошелька не изменился", walletCampaign.getTotalSum(), equalTo(BigDecimal.ZERO.setScale(6)));
    }
}
