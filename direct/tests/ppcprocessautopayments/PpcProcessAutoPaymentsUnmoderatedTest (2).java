package ru.yandex.autotests.directintapi.tests.ppcprocessautopayments;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletCampaignsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletPaymentTransactionsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.autotests.directapi.model.balancesimple.response.BindCreditCardResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 06/04/16
 * https://st.yandex-team.ru/TESTIRT-8967
 * Для определения "модерированности для баланса" используется готовая функция, которая, если опустить детали,
 * считает кошелек промодерированным, если хотя бы одна кампания под кошельком удовлетворяет условиям:
 * сервисируемая
 * ИЛИ
 * агентская
 * ИЛИ
 * statusPostModerate == Accepted (т.е. предварительно принята на модерации)
 * ИЛИ
 * statusPostModerate != Yes И StatusModerate !~ /^(No|Ready|Sent|New)$/
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_PROCESS_AUTO_PAYMENTS)
@Description("Автопополение с непромодерированным ОС")
@Issue("https://st.yandex-team.ru/DIRECT-52054")
public class PpcProcessAutoPaymentsUnmoderatedTest {
    private static final String LOGIN = Logins.AUTOPAY_LOGIN15;
    private static final BigDecimal PAY_SUM = MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().bigDecimalValue();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private DirectJooqDbSteps dbSteps;
    private Integer shard;
    private Long walletCid;
    private String paymentMethod;

    private WalletCampaignsRecord walletCampaigns;

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        //Remove after Balance DB update
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        dbSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShard(shard);
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        BindCreditCardResponse bindCreditCardResponse = api.userSteps.balanceSimpleSteps().bindCreditCard(
                new BindCreditCardRequest().defaultCreditCard());
        paymentMethod = bindCreditCardResponse.getPaymentMethod();
        dbSteps.walletPaymentTransactionsSteps().deleteWalletPaymentTransactionsByCid(walletCid);
        walletCampaigns = dbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);

        dbSteps.walletCampaignsSteps().updateWalletCampaigns(
                walletCampaigns.setAutopayMode(WalletCampaignsAutopayMode.min_balance)
        );

    }

    @Test
    @Description("Создание первой транзакции")
    public void testWalletPaymentTransactionsAfterFirstCall() {
        dbSteps.autopaySettingsSteps().saveDefaultCardAutopaySettings(walletCid
                , Long.valueOf(User.get(LOGIN).getPassportUID()), paymentMethod
                , walletCampaigns.getTotalSum(), PAY_SUM, 0);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcProcessAutoPayments(shard, walletCid);

        List<WalletPaymentTransactionsRecord> walletPaymentTransactionsRecords
                = dbSteps.walletPaymentTransactionsSteps().getWalletPaymentTransactionsByWalletCid(walletCid);
        assertThat("запрос на автопополнение не отправлен", walletPaymentTransactionsRecords
                , iterableWithSize(0));
    }

    @After
    public void clear() {
        dbSteps.autopaySettingsSteps().deleteAutopaySettings(walletCid);
    }
}
