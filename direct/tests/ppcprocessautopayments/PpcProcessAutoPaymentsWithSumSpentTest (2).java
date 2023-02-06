package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
@Description("Полный успешный цикл автопополения")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
@RunWith(Parameterized.class)
public class PpcProcessAutoPaymentsWithSumSpentTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN10;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();
    private static final Money AUTOPAY_REMAINING_SUM = Money.valueOf(PAY_SUM).multiply(2L).addVAT(Currency.RUB);
    private static final Long INITIAL_BALANCE_TID = DateTime.now().getMillis() * 10;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DirectJooqDbSteps dbSteps;
    private static Integer shard;
    private static Long walletCid;
    private static Long cid;
    private static String paymentMethod;

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public Money totalSum;
    @Parameterized.Parameter(2)
    public Money walletSumSpent;
    @Parameterized.Parameter(3)
    public Money campaignSumSpent;
    @Parameterized.Parameter(4)
    public int expectedTransactionsNum;

    @Parameterized.Parameters(name = "{0}")
    public static Collection feeds() {
        Object[][] data = new Object[][]{
                {"Сумма на ОС с учетом откруток больше установленного лимита автопополнения"
                        , AUTOPAY_REMAINING_SUM.multiply(2f).add(BigDecimal.ONE)
                        , AUTOPAY_REMAINING_SUM.divide(2f).subtract(BigDecimal.ONE)
                        , AUTOPAY_REMAINING_SUM.divide(2f).subtract(BigDecimal.ONE)
                        , 0},
                {"Сумма на ОС с учетом откруток меньше установленного лимита автопополнения, открутки только на ОС"
                        , AUTOPAY_REMAINING_SUM.multiply(2f)
                        , AUTOPAY_REMAINING_SUM.add(BigDecimal.ONE)
                        , Money.valueOf(0L)
                        , 1},
                {"Сумма на ОС с учетом откруток меньше установленного лимита автопополнения" +
                        ", открутки только на кампании"
                        , AUTOPAY_REMAINING_SUM.multiply(2f)
                        , Money.valueOf(0L)
                        , AUTOPAY_REMAINING_SUM.add(BigDecimal.ONE)
                        , 1},
                {"Сумма на ОС с учетом откруток меньше установленного лимита автопополнения, открутки и на ОС" +
                        ", и на кампании"
                        , AUTOPAY_REMAINING_SUM.multiply(2f)
                        , AUTOPAY_REMAINING_SUM.divide(2f).add(BigDecimal.ONE)
                        , AUTOPAY_REMAINING_SUM.divide(2f)
                        , 1},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard());
        paymentMethod = bindCreditCardResponse.getPaymentMethod();
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid)
                        .setAutopayMode(WalletCampaignsAutopayMode.min_balance)
                        .setTotalBalanceTid(INITIAL_BALANCE_TID)
                        .setTotalSum(totalSum.bigDecimalValue())
        );
        api.userSteps.campaignFakeSteps().setSumSpent(walletCid.intValue(), walletSumSpent.floatValue());
        api.userSteps.campaignFakeSteps().setSumSpent(cid.intValue(), campaignSumSpent.floatValue());
        dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , AUTOPAY_REMAINING_SUM.subtractVAT(Currency.RUB).bigDecimalValue(), PAY_SUM, 0);
    }

    @Test
    public void testWalletPaymentTransactionsWithSumSpent() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("в базе верное количество транзакций", walletPaymentTransactionsRecords
                , iterableWithSize(expectedTransactionsNum));
    }

    @After
    public void after() {
        api.userSteps.campaignFakeSteps().setSumSpent(cid.intValue(), 0);
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }
}
