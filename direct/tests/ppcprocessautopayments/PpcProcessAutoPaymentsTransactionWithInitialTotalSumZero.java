package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletPaymentTransactionsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.GetRequestMap;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.autotests.directapi.model.balancesimple.response.BindCreditCardResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by buhter on 06/04/16
 * https://st.yandex-team.ru/TESTIRT-8967
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_PROCESS_AUTO_PAYMENTS)
@Description("Клиент с wallet_campaigns.total_sum = 0")
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-52054"),
        @Issue("https://st.yandex-team.ru/DIRECT-54213")})
public class PpcProcessAutoPaymentsTransactionWithInitialTotalSumZero {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN14;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DirectJooqDbSteps dbSteps;
    private static Integer shard;
    private static Long walletCid;
    private static String paymentMethod;
    private static Long uid;
    private List<Long> campaignIds;

    @BeforeClass
    public static void before() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        uid = Long.valueOf(User.get(LOGIN).getPassportUID());
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);
        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard().withExpirationYear("19"));
        paymentMethod = bindCreditCardResponse.getPaymentMethod();
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void init() {
        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid)
                        .setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                        .setTotalSum(BigDecimal.ZERO)
                        .setTotalBalanceTid(0L)
        );
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);
        dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid, uid, paymentMethod
                , PAY_SUM, PAY_SUM, 0);
        List<CampaignGetItem> campaignGetItems = api.userSteps.campaignSteps()
                .campaignsGet(new GetRequestMap()
                        .withFieldNames(CampaignFieldEnum.ID)
                        .withSelectionCriteria(new CampaignsSelectionCriteriaMap())
                ).getCampaigns();
        assumeThat("список кампаний пользователя не пустой", campaignGetItems, iterableWithSize(greaterThan(0)));
        campaignIds = campaignGetItems.stream().map(CampaignGetItem::getId).collect(Collectors.toList());
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(campaignIds.get(0));
    }

    @Test
    public void testZeroTotalSumForClientWithMoneyEnabledWallet() {
        for (Long campaignId : campaignIds) {
            api.userSteps.campaignFakeSteps().setCampaignSum(campaignId, PAY_SUM.floatValue());
            api.userSteps.campaignFakeSteps().setSumSpent(campaignId, Money.valueOf(PAY_SUM).getPrevious().floatValue());
        }
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, 0f);
        api.userSteps.campaignFakeSteps().setSumSpent(walletCid, 0f);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("запрос на автопополнение не был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(0));
    }

    @Test
    public void testZeroTotalSumForClientWithMoneyEnabledWalletAndAllMoneySpent() {
        for (Long campaignId : campaignIds) {
            api.userSteps.campaignFakeSteps().setCampaignSum(campaignId, PAY_SUM.floatValue());
            api.userSteps.campaignFakeSteps().setSumSpent(campaignId, PAY_SUM.floatValue());
        }
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, 0f);
        api.userSteps.campaignFakeSteps().setSumSpent(walletCid, 0f);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("запрос на автопополнение был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
    }

    @Test
    public void testZeroTotalSumForNewClient() {
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, 0f);
        api.userSteps.campaignFakeSteps().setSumSpent(walletCid, 0f);
        for (Long campaignId : campaignIds) {
            api.userSteps.campaignFakeSteps().setCampaignSum(campaignId, 0f);
            api.userSteps.campaignFakeSteps().setSumSpent(campaignId, 0f);
        }
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, 0f);
        api.userSteps.campaignFakeSteps().setSumSpent(walletCid, 0f);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("запрос на автопополнение был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
    }

    @After
    public void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }

}
