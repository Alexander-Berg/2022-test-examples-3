package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
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
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.WalletPaymentTransactionBalanceStatus;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.autotests.directapi.model.balancesimple.response.BindCreditCardResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
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
 * Created by buhter on 18/05/16
 * https://st.yandex-team.ru/TESTIRT-8967
 * https://st.yandex-team.ru/TESTIRT-10284
 * https://st.yandex-team.ru/TESTIRT-10299
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.NOT_REGRESSION_YET)
@Description("Не завершена предыдущая транзакция")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
public class PpcProcessAutoPaymentsWithProcessingTransactionsTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN12;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();
    private static final Long INITIAL_BALANCE_TID = DateTime.now().getMillis() * 10;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private DirectJooqDbSteps dbSteps;
    private Integer shard;
    private Long walletCid;

    private WalletPaymentTransactionsRecord expected = new WalletPaymentTransactionsRecord();

    @Before
    @Step("Подготовка тестовых данных")
    public void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, Money.valueOf(PAY_SUM)
                .addVAT(Currency.RUB).floatValue());
        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard());
        String paymentMethod = bindCreditCardResponse.getPaymentMethod();

        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);

        WalletCampaignsRecord walletCampaigns = dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(walletCampaigns
                .setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                .setTotalBalanceTid(INITIAL_BALANCE_TID)
        );

        dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , walletCampaigns.getTotalSum(), PAY_SUM, 0);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
    }

    @Test
    @Description("Не создается транзакция, если предыдущая не завершена")
    public void testWalletPaymentTransactionsNotCreatedWhilePreviousIsProcessing() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        expected = new WalletPaymentTransactionsRecord().setWalletCid(walletCid)
                .setPayerUid(Long.valueOf(User.get(LOGIN).getPassportUID()))
                .setBalanceStatus(WalletPaymentTransactionBalanceStatus.WAIT_FOR_NOTIFICATION.toString())
                .setStatus(WalletPaymentTransactionsStatus.Processing)
                .setTotalBalanceTid(INITIAL_BALANCE_TID);

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assumeThat("есть ровно один запрос на автопополнение"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
        assertThat("запрос на автопополнение соответствует ожиданиям", walletPaymentTransactionsRecords.get(0).intoMap()
                , beanDiffer(expected.intoMap()).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @After
    public void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }
}
