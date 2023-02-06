package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.sql.Timestamp;

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
import ru.yandex.autotests.directapi.steps.ConditionFactories;
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
 * Created by buhter on 06/04/16
 * https://st.yandex-team.ru/TESTIRT-8967
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_PROCESS_AUTO_PAYMENTS)
@Description("Сброс счетчика попыток при повторном успешном автопополнении")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
public class PpcProcessAutoPaymentsTriesCountResetAfterSuccessTransactionTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN5;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DirectJooqDbSteps dbSteps;
    private static Integer shard;
    private static Long walletCid;
    private static Account account;

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);

        account = api.userSteps.financeSteps().getAccount(walletCid.intValue());
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid)
                        .setAutopayMode(WalletCampaignsAutopayMode.min_balance));

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard().withExpirationYear("19"));
        String paymentMethod = bindCreditCardResponse.getPaymentMethod();

        dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , Money.valueOf(account.getAmount()).bigDecimalValue(), PAY_SUM, 1);

        WalletPaymentTransactionsRecord walletPaymentTransactionsWithError = dbSteps.walletPaymentTransactionsSteps()
                .saveDefaultAutoWalletPaymentTransactions(walletCid, Long.valueOf(User.get(LOGIN).getPassportUID())
                        , WalletPaymentTransactionsStatus.Error
                        , WalletPaymentTransactionBalanceStatus.ERROR.toString());

        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions(
                walletPaymentTransactionsWithError
                        .setBalanceStatusCode(
                                WalletPaymentTransactionBalanceStatusCode.PAYMENT_TIMEOUT.toString()
                        )
                        .setBalanceStatus(WalletPaymentTransactionsStatus.Error.getLiteral().toLowerCase())
                        .setCreateTime(Timestamp.valueOf(
                                walletPaymentTransactionsWithError.getCreateTime().toLocalDateTime().minusHours(6))
                        )
        );

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
    }

    @Test
    public void testWalletPaymentTransactionsAfterSecondCall() {
        ConditionFactories.NOTIFY_PAYMENT.until(
                api.userSteps.financeSteps().accountAmountChanged(account, true)
        );

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);

        AutopaySettingsRecord autopaySettings = dbSteps.autopaySettingsSteps().getAutopaySettings(walletCid);
        assertThat("счетчик попыток сброшен", autopaySettings.getTriesNum(), equalTo(0));
    }

    @After
    public void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }
}
