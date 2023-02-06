package ru.yandex.autotests.direct.intapi.java.tests.balance;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsIsSumAggregated;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.closeTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("NotifyOrder2 - поведение при использовании новой схемы для общего счёта")
@Features(TestFeatures.BALANCE_NOTIFY_ORDER_2)
@Tag(Tags.BALANCE_NOTIFY_ORDER_2)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-86523")
public class NotifyOrder2AggregateMigrationTest {

    private static final LogSteps log = LogSteps.getLogger(NotifyOrder2AggregateMigrationTest.class);

    private static final BigDecimal DELTA = BigDecimal.valueOf(0.001);

    private static final String LOGIN = "at-intapi-wallet2";
    private static final float DEFAULT_CONSUME_QTY = 100f;
    private static final float DEFAULT_TOTAL_CONSUME_QTY = 200f;

    private static final BigDecimal DEFAULT_SUM = BigDecimal.valueOf(10_500);

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private long campaignId;
    private long walletCid;
    private Long walletIdLock;

    @Rule
    public Trashman trasher = new Trashman(api);

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private DirectJooqDbSteps jooqDbSteps;

    @Before
    public void init() {
        log.info("Подготовка данных для теста");

        jooqDbSteps = api.userSteps.getDirectJooqDbSteps();
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        int shard = api.userSteps.getDBSteps().getShardingSteps().getShardByCid(campaignId);

        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        CampaignsRecord campaign = jooqDbSteps.useShard(shard).campaignsSteps().getCampaignById(campaignId);
        campaign.setWalletCid(walletCid);
        campaign.setSum(DEFAULT_SUM);
        campaign.setSumBalance(BigDecimal.ZERO);

        jooqDbSteps.campaignsSteps().updateCampaigns(campaign);
        updateStatusMigrating(walletCid, WalletCampaignsIsSumAggregated.No);
    }

    @After
    public void after() {
        if (walletIdLock != null) {
            directClassRule.intapiSteps().balanceClientSteps().redisLock(false, walletIdLock);
        }
    }

    @Test
    public void notifyOrder2_WalletMigrating_MigratingError() {
        updateStatusMigrating(walletCid, WalletCampaignsIsSumAggregated.Migrating);

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(buildRequest(walletCid),
                400, 810, "Group Order " + walletCid + " is migrating to aggregated state"
        );
        CampaignsRecord campaign = jooqDbSteps.campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании не изменился", campaign.getSum(), closeTo(DEFAULT_SUM, DELTA));
    }

    @Test
    public void notifyOrder2_CampaignUnderWalletMigrating_MigratingError() {
        updateStatusMigrating(walletCid, WalletCampaignsIsSumAggregated.Migrating);

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(buildRequest(campaignId),
                400, 810, "Group Order " + campaignId + " is migrating to aggregated state"
        );
        CampaignsRecord campaign = jooqDbSteps.campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании не изменился", campaign.getSum(), closeTo(DEFAULT_SUM, DELTA));
    }

    @Test
    public void notifyOrder2_UpdateCampaignUnderMigratingWallet_LockTroublesError() {
        lock(walletCid);

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(buildRequest(walletCid),
                400, 811, "Try again later");
        CampaignsRecord campaign = jooqDbSteps.campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании не изменился", campaign.getSum(), closeTo(DEFAULT_SUM, DELTA));
    }

    @Test
    public void notifyOrder2_UpdateMigratingWallet_LockTroublesError() {
        lock(walletCid);

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(buildRequest(walletCid),
                400, 811, "Try again later");
        CampaignsRecord campaign = jooqDbSteps.campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании не изменился", campaign.getSum(), closeTo(DEFAULT_SUM, DELTA));
    }

    @Test
    public void notifyOrder2_CampaignInNewSchema_UpdatedBalanceSum() {
        updateStatusMigrating(walletCid, WalletCampaignsIsSumAggregated.Yes);

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(buildRequest(campaignId));

        CampaignsRecord campaign = jooqDbSteps.campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании равен нулю", campaign.getSum(), closeTo(BigDecimal.ZERO, DELTA));
        assertThat("Общий баланс кампании равен отправленному значению в нотификации", campaign.getSumBalance(),
                closeTo(BigDecimal.valueOf(DEFAULT_CONSUME_QTY), DELTA));
    }

    @Test
    public void notifyOrder2_WalletInNewSchema_UpdatedSumAndSumBalance() {
        updateStatusMigrating(walletCid, WalletCampaignsIsSumAggregated.Yes);

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(buildRequest(walletCid));

        CampaignsRecord campaign = jooqDbSteps.campaignsSteps().getCampaignById(walletCid);
        assertThat("Баланс кампании равен нулю", campaign.getSum(),
                closeTo(BigDecimal.valueOf(DEFAULT_TOTAL_CONSUME_QTY), DELTA));
        assertThat("Общий баланс кампании равен отправленному значению в нотификации", campaign.getSumBalance(),
                closeTo(BigDecimal.valueOf(DEFAULT_CONSUME_QTY), DELTA));
    }

    private void lock(long walletId) {
        directClassRule.intapiSteps().balanceClientSteps().redisLock(true, walletId);
        walletIdLock = walletId;
    }

    private void updateStatusMigrating(long walletCid, WalletCampaignsIsSumAggregated status) {
        jooqDbSteps.walletCampaignsSteps().updateWalletCampaigns(
                jooqDbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid).setIsSumAggregated(status));
    }

    private NotifyOrder2JSONRequest buildRequest(long campaignId) {
        return new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTimestamp()
                .withConsumeQty(DEFAULT_CONSUME_QTY)
                .withTotalConsumeQty(DEFAULT_TOTAL_CONSUME_QTY)
                .withProductCurrency(Currency.RUB.value());
    }
}
