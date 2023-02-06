package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletPaymentTransactionsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutopaySettingsRecord;
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
 * https://st.yandex-team.ru/TESTIRT-8967
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_PROCESS_AUTO_PAYMENTS)
@Description("Ошибка требующая вмешательства человека")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
public class PpcProcessAutoPaymentsTransactionWithErrorTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN4;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();
    private static final Long INITIAL_BALANCE_TID = DateTime.now().getMillis() * 10;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private DirectJooqDbSteps dbSteps;
    private Integer shard;
    private Long walletCid;
    private AutopaySettingsRecord autopaySettings;

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        Long uid = Long.valueOf(User.get(LOGIN).getPassportUID());
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);

        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        dbSteps.campaignsSteps().updateCampaigns(
                dbSteps.campaignsSteps().getCampaignById(walletCid).setBalanceTid(INITIAL_BALANCE_TID)
        );

        Account account = api.userSteps.financeSteps().getAccount(walletCid.intValue());

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard().withExpirationYear("19"));
        String paymentMethod = bindCreditCardResponse.getPaymentMethod();

        autopaySettings = dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid, uid, paymentMethod
                , Money.valueOf(account.getAmount()).bigDecimalValue(), PAY_SUM, 1);

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid)
                        .setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                        .setTotalBalanceTid(INITIAL_BALANCE_TID)
        );

        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);

        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions(
                dbSteps.walletPaymentTransactionsSteps()
                        .saveDefaultAutoWalletPaymentTransactions(walletCid, uid, WalletPaymentTransactionsStatus.Error
                                , WalletPaymentTransactionBalanceStatus.ERROR.toString())
                        .setBalanceStatusCode(WalletPaymentTransactionBalanceStatusCode.EXPIRED_CARD.toString())
        );
    }

    @Test
    public void testNegativeWalletPaymentTransactionsWithPreviousError() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("повторный запрос на автопополнение не был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
    }

    @Test
    public void testNegativeWalletPaymentTransactionsWithPreviousErrorAndZeroTriesCount() {
        dbSteps.autopaySettingsSteps().updateAutopaySettings(autopaySettings.setTriesNum(0));
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("повторный запрос на автопополнение был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(2));
    }

    @Test
    public void testNegativeWalletPaymentTransactionsWithPreviousErrorAndMuchTimeSpent() {
        WalletPaymentTransactionsRecord walletPaymentTransactionsWithError
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid).get(0);
        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions(
                walletPaymentTransactionsWithError
                        .setCreateTime(Timestamp.valueOf(
                                walletPaymentTransactionsWithError
                                        .getCreateTime().toLocalDateTime().minusHours(7)))
        );

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("повторный запрос на автопополнение не был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
    }

    @After
    public void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }

}
