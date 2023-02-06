package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletPaymentTransactionsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletCampaignsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletPaymentTransactionsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45.Account;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.WalletPaymentTransactionBalanceStatus;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.autotests.directapi.model.balancesimple.response.BindCreditCardResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.steps.ConditionFactories;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 06/04/16
 * https://st.yandex-team.ru/TESTIRT-8967
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_PROCESS_AUTO_PAYMENTS)
@Description("Автоплатеж отключен")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
public class PpcProcessAutoPaymentsWithNoAutoPaymentAndProcessingTransactionsTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN9;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DirectJooqDbSteps dbSteps;
    private static Integer shard;
    private static Long walletCid;
    private static Account account;
    private static String paymentMethod;

    private WalletCampaignsRecord walletCampaigns;
    private WalletPaymentTransactionsRecord expected = new WalletPaymentTransactionsRecord();

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);
        account = api.userSteps.financeSteps().getAccount(walletCid.intValue());
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, Money.valueOf(PAY_SUM)
                .addVAT(Currency.RUB).floatValue());
        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard());
        paymentMethod = bindCreditCardResponse.getPaymentMethod();
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);

        walletCampaigns = dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(walletCampaigns
                .setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                .setTotalBalanceTid(1L)
        );

        dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , walletCampaigns.getTotalSum(), PAY_SUM, 0);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
    }

    @Test
    @Description("Подтверждение транзакции при выключенном автопополнении")
    public void testWalletPaymentTransactionsFinishWithTurnedOffAutoPayment() {

        ConditionFactories.NOTIFY_PAYMENT.until(
                api.userSteps.financeSteps().accountAmountChanged(account, true)
        );

        dbSteps.walletCampaignsSteps()
                .updateWalletCampaigns(walletCampaigns.setAutopayMode(WalletCampaignsAutopayMode.none));

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);

        expected.setWalletCid(walletCid)
                .setPayerUid(Long.valueOf(User.get(LOGIN).getPassportUID()))
                .setBalanceStatus(WalletPaymentTransactionBalanceStatus.SUCCESS.toString())
                .setStatus(WalletPaymentTransactionsStatus.Done);

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assumeThat("остался все еще один запрос на автопополнение"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
        assertThat("запрос на автопополнение соответствует ожиданиям", walletPaymentTransactionsRecords.get(0).intoMap()
                , beanDiffer(expected.intoMap()).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @After
    public void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }

}
