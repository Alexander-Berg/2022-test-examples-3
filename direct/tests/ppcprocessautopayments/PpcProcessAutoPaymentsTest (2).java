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
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutopaySettingsRecord;
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
@Description("Полный успешный цикл автопополения")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
public class PpcProcessAutoPaymentsTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN1;
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

    private WalletPaymentTransactionsRecord expected = new WalletPaymentTransactionsRecord();
    private WalletCampaignsRecord walletCampaigns;
    private AutopaySettingsRecord autopaySettings;

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(cid);

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard());
        paymentMethod = bindCreditCardResponse.getPaymentMethod();
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        getAccount();
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);
        walletCampaigns = dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                walletCampaigns.setAutopayMode(WalletCampaignsAutopayMode.min_balance)
        );

        autopaySettings = dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , Money.valueOf(walletCampaigns.getTotalSum()).bigDecimalValue()
                , PAY_SUM, 0);

    }

    @Test
    @Description("Создание первой транзакции")
    public void testWalletPaymentTransactionsAfterFirstCall() {
        executeScriptAndWaitForBalanceNotification();

        expected.setWalletCid(walletCid)
                .setPayerUid(Long.valueOf(User.get(LOGIN).getPassportUID()))
                .setBalanceStatus("wait_for_notification")
                .setStatus(WalletPaymentTransactionsStatus.Processing)
                .setTotalBalanceTid(walletCampaigns.getTotalBalanceTid());

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assumeThat("отправлен ровно один запрос на автопополнение", walletPaymentTransactionsRecords
                , iterableWithSize(1));
        assertThat("запрос на автопополнение соответствует ожиданиям", walletPaymentTransactionsRecords.get(0).intoMap()
                , beanDiffer(expected.intoMap()).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("Подтверждение первой транзакции и создание новой при наличии средств все еще меньше минимума")
    public void testWalletPaymentTransactionsAfterSecondCallWithShortOfMoney() {
        executeScriptAndWaitForBalanceNotification();

        dbSteps.autopaySettingsSteps().updateAutopaySettings(autopaySettings
                .setRemainingSum(Money.valueOf(walletCampaigns.getTotalSum())
                        .add(PAY_SUM).add(PAY_SUM).bigDecimalValue()));
        executeScriptAndWaitForBalanceNotification();

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assumeThat("появился еще один запрос на автопополнение"
                , walletPaymentTransactionsRecords, iterableWithSize(2));
    }

    private void getAccount() {
        account = api.userSteps.financeSteps().getAccount(walletCid.intValue());
    }

    private void executeScriptAndWaitForBalanceNotification() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);
        ConditionFactories.NOTIFY_PAYMENT.until(
                api.userSteps.financeSteps().accountAmountChanged(account, true)
        );
        // обновлеяем значение, чтобы в следующий раз корректно дождаться нотификации (если потребуется)
        getAccount();
    }

    @After
    public void clear() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }
}
