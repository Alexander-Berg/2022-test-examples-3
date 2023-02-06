package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletPaymentTransactionsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutopaySettingsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletPaymentTransactionsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.WalletPaymentTransactionBalanceStatus;
import ru.yandex.autotests.directapi.darkside.model.WalletPaymentTransactionBalanceStatusCode;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.autotests.directapi.model.balancesimple.response.BindCreditCardResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 06/04/16
 * https://st.yandex-team.ru/TESTIRT-9414
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.PPC_PROCESS_AUTO_PAYMENTS)
@Description("Автопополение, проверка предохранителя от тормозящих нотификаций")
@Issue("https://st.yandex-team.ru/DIRECT-54711")

public class PpcProcessAutoPaymentsWaitForNotificationsTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN11;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();
    private static final Long INITIAL_BALANCE_TID = DateTime.now().getMillis() * 10;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DirectJooqDbSteps dbSteps;
    private static Integer shard;
    private static Long walletCid;
    private AutopaySettingsRecord autopaySettings;

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);
        dbSteps.campaignsSteps().updateCampaigns(
                dbSteps.campaignsSteps().getCampaignById(walletCid).setBalanceTid(INITIAL_BALANCE_TID)
        );

    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard().withExpirationYear("19"));
        String paymentMethod = bindCreditCardResponse.getPaymentMethod();

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid)
                        .setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                        .setTotalBalanceTid(INITIAL_BALANCE_TID)
                        .setTotalSum(PAY_SUM.subtract(BigDecimal.ONE))
        );

//        Account account = api.userSteps.financeSteps().getAccount(walletCid.intValue());
        autopaySettings = dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , PAY_SUM, PAY_SUM, 0);

    }

    @Test
    @Description("wallet_payment_transactions.total_balance_tid == wallet_campaigns.total_balance_tid " +
            "и последняя транзакция успешна; Ожидаем, что новый запрос на автопополение не создастся")
    public void testWalletPaymentTransactionsIfBalanceTidForWalletAndTransactionAreEqual() {
        dbSteps.walletPaymentTransactionsSteps()
                .saveDefaultAutoWalletPaymentTransactions(walletCid, Long.valueOf(User.get(LOGIN).getPassportUID())
                        , WalletPaymentTransactionsStatus.Done
                        , WalletPaymentTransactionBalanceStatus.SUCCESS.toString()
                        , INITIAL_BALANCE_TID);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid, true);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("кол-во транзакций соответствует ожиданиям", walletPaymentTransactionsRecords
                , iterableWithSize(1));
    }

    @Test
    @Description("wallet_payment_transactions.total_balance_tid < wallet_campaigns.total_balance_tid " +
            "и последняя транзакция успешна; Ожидаем новый запрос на автопополение")
    public void testWalletPaymentTransactionsIfBalanceTidForWalletAndTransactionAreNotEqual() {
        dbSteps.walletPaymentTransactionsSteps()
                .saveDefaultAutoWalletPaymentTransactions(walletCid, Long.valueOf(User.get(LOGIN).getPassportUID())
                        , WalletPaymentTransactionsStatus.Done
                        , WalletPaymentTransactionBalanceStatus.SUCCESS.toString()
                        , INITIAL_BALANCE_TID - 1);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid, true);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("кол-во транзакций соответствует ожиданиям", walletPaymentTransactionsRecords
                , iterableWithSize(2));
    }

    @Test
    @Description("wallet_payment_transactions.total_balance_tid == wallet_campaigns.total_balance_tid " +
            "и последняя транзакция неуспешна (но не требует человеческого вмешательства); " +
            "Ожидаем новый запрос на автопополение")
    public void testWalletPaymentTransactionsIfBalanceTidForWalletAndTransactionAreEqualWithLastError() {
        dbSteps.autopaySettingsSteps().updateAutopaySettings(autopaySettings.setTriesNum(1));
        WalletPaymentTransactionsRecord walletPaymentTransactionsWithError = dbSteps.walletPaymentTransactionsSteps()
                .saveDefaultAutoWalletPaymentTransactions(walletCid, Long.valueOf(User.get(LOGIN).getPassportUID())
                        , WalletPaymentTransactionsStatus.Error
                        , WalletPaymentTransactionBalanceStatus.ERROR.toString()
                        , INITIAL_BALANCE_TID)
                .setBalanceStatusCode(WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR.toString())
                .setBalanceStatus(WalletPaymentTransactionsStatus.Error.getLiteral().toLowerCase());

        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions
                (walletPaymentTransactionsWithError.setCreateTime(Timestamp.valueOf(walletPaymentTransactionsWithError
                        .getCreateTime().toLocalDateTime().minusHours(6)))
                );


        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid, true);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("кол-во транзакций соответствует ожиданиям", walletPaymentTransactionsRecords
                , iterableWithSize(2));
    }

    @Test
    @Description("wallet_payment_transactions.total_balance_tid < wallet_campaigns.total_balance_tid " +
            "и последняя транзакция неуспешна (но не требует человеческого вмешательства); " +
            "Ожидаем новый запрос на автопополнение")
    public void testWalletPaymentTransactionsIfBalanceTidForWalletAndTransactionAreNotEqualWithLastError() {
        dbSteps.autopaySettingsSteps().updateAutopaySettings(autopaySettings.setTriesNum(1));
        WalletPaymentTransactionsRecord walletPaymentTransactionsWithError = dbSteps.walletPaymentTransactionsSteps()
                .saveDefaultAutoWalletPaymentTransactions(walletCid, Long.valueOf(User.get(LOGIN).getPassportUID())
                        , WalletPaymentTransactionsStatus.Error
                        , WalletPaymentTransactionBalanceStatus.ERROR.toString()
                        , INITIAL_BALANCE_TID - 1)
                .setBalanceStatusCode(WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR.toString())
                .setBalanceStatus(WalletPaymentTransactionsStatus.Error.getLiteral().toLowerCase());

        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions
                (walletPaymentTransactionsWithError.setCreateTime(Timestamp.valueOf(walletPaymentTransactionsWithError
                        .getCreateTime().toLocalDateTime().minusHours(6)))
                );


        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid, true);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("кол-во транзакций соответствует ожиданиям", walletPaymentTransactionsRecords
                , iterableWithSize(2));
    }

    @After
    public void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }

}
