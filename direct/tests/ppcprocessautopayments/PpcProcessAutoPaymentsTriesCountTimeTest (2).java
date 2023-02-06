package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletPaymentTransactionsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutopaySettingsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletCampaignsRecord;
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

import static org.hamcrest.Matchers.equalTo;
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
@Description("Увеличение счетчика попыток автопополения и их временные промежутки")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
@RunWith(Parameterized.class)
public class PpcProcessAutoPaymentsTriesCountTimeTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN8;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();
    private static final Long INITIAL_BALANCE_TID = DateTime.now().getMillis() * 10;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DirectJooqDbSteps dbSteps;
    private static Integer shard;
    private static Long uid;
    private static Long walletCid;
    private static AutopaySettingsRecord autopaySettings;
    private static WalletCampaignsRecord walletCampaigns;

    @Parameterized.Parameter(0)
    public int triesNum;
    @Parameterized.Parameter(1)
    public WalletPaymentTransactionBalanceStatusCode balanceStatus;
    @Parameterized.Parameter(2)
    public int timeoutHours;
    @Parameterized.Parameter(3)
    public boolean expectNewTransaction;

    private WalletPaymentTransactionsRecord walletPaymentTransactionsWithError;

    @Parameterized.Parameters(name = "количество попыток = {0}, ошибка из баланса = {1}" +
            ", времени после последней попытки(часы) = {2}")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {1, WalletPaymentTransactionBalanceStatusCode.NOT_ENOUGH_FUNDS, 6, true},
                {1, WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR, 6, true},
                {1, WalletPaymentTransactionBalanceStatusCode.PAYMENT_REFUSED, 6, true},
                {1, WalletPaymentTransactionBalanceStatusCode.AUTHORIZATION_REJECT, 6, true},
                {1, WalletPaymentTransactionBalanceStatusCode.PAYMENT_TIMEOUT, 6, true},
                {1, WalletPaymentTransactionBalanceStatusCode.DECLINED_BY_ISSUER, 6, true},
                {2, WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR, 12, true},
                {3, WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR, 24, true},
                {4, WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR, 48, true},
                {5, WalletPaymentTransactionBalanceStatusCode.TECHNICAL_ERROR, 96, false},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        uid = Long.valueOf(User.get(LOGIN).getPassportUID());
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        dbSteps.campaignsSteps().updateCampaigns(
                dbSteps.campaignsSteps().getCampaignById(walletCid).setBalanceTid(INITIAL_BALANCE_TID)
        );

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard());
        String paymentMethod = bindCreditCardResponse.getPaymentMethod();

        walletCampaigns = dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);
        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                walletCampaigns.setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                        .setTotalBalanceTid(INITIAL_BALANCE_TID)
        );
        autopaySettings = dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid, uid, paymentMethod
                , walletCampaigns.getTotalSum(), PAY_SUM, 0);
    }

    @AfterClass
    public static void after() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);
        walletCampaigns = dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);
        walletPaymentTransactionsWithError = dbSteps.walletPaymentTransactionsSteps()
                .saveDefaultAutoWalletPaymentTransactions(walletCid, uid, WalletPaymentTransactionsStatus.Error
                        , WalletPaymentTransactionBalanceStatus.ERROR.toString())
                .setBalanceStatusCode(balanceStatus.toString())
                .setBalanceStatus(WalletPaymentTransactionsStatus.Error.getLiteral().toLowerCase());
    }

    @Test
    public void testSuccessWalletPaymentTransactionsBeforeTime() {
        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions
                (walletPaymentTransactionsWithError.setCreateTime(Timestamp.valueOf(walletPaymentTransactionsWithError
                        .getCreateTime().toLocalDateTime().minusHours(timeoutHours).plusMinutes(1)))
                );
        dbSteps.autopaySettingsSteps().updateAutopaySettings(
                autopaySettings
                        .setTriesNum(triesNum)
                        .setRemainingSum(walletCampaigns.getTotalSum())
        );

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("повторный запрос на автопополнение не был отправлен"
                , walletPaymentTransactionsRecords, iterableWithSize(1));
    }

    @Test
    public void testSuccessWalletPaymentTransactionsAfterTime() {
        dbSteps.walletPaymentTransactionsSteps().updateWalletPaymentTransactions(
                walletPaymentTransactionsWithError.setCreateTime(Timestamp.valueOf(walletPaymentTransactionsWithError
                        .getCreateTime().toLocalDateTime().minusHours(timeoutHours)))
        );
        dbSteps.autopaySettingsSteps().updateAutopaySettings(
                autopaySettings
                        .setTriesNum(triesNum)
                        .setRemainingSum(walletCampaigns.getTotalSum())
        );

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);

        autopaySettings = dbSteps.autopaySettingsSteps().getAutopaySettings(walletCid);
        if (expectNewTransaction) {
            assumeThat("счетчик попыток увеличился", autopaySettings.getTriesNum(), equalTo(triesNum + 1));
        } else {
            assumeThat("счетчик попыток не увеличился", autopaySettings.getTriesNum(), equalTo(triesNum));
        }
        assertThat("повторный запрос на автопополнение был отправлен"
                , walletPaymentTransactionsRecords, expectNewTransaction ? iterableWithSize(2) : iterableWithSize(1));
    }
}
