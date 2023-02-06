package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.directapi.common.api45.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45.Account;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.tags.StageTag;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: pavryabov
 * Date: 19.03.13
 * https://st.yandex-team.ru/TESTIRT-10470
 */

@Aqua.Test(title = "NotifyOrder2 - валютный клиент")
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-54789"),
        @Issue("https://st.yandex-team.ru/BALANCE-23760")})
@Tag(StageTag.RELEASE)
@Tag(StageTag.TRUNK)
@Features(FeatureNames.NOT_REGRESSION_YET)
@Description("для Белорусских клинетов принимаются нотификации с ProductCurrency = '' и ConsumeCurrency = RUB | BYN")
@RunWith(Parameterized.class)
public class NotifyOrder2CurrencyBYNTest {
    private static final String LOGIN = Logins.LOGIN_BYN;
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private Long campaignId;
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2CurrencyBYNTest.class);

    @Parameterized.Parameter(0)
    public Currency productCurrency;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN)
            .wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameters(name = "Отправляем NotifyOrder2 c ProductCurrency = {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{Currency.BYN},
                new Object[]{Currency.YND_FIXED});
    }

    @Before
    public void init() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
    }

    @Test
    public void notifyOrder2WithCurrencyTest() {
        Money qty = Money.valueOf(100.0f, Currency.BYN);
        log.info("Вызываем метод NotifyOrder2 для белорусского клиента. Оплачиваем кампанию под кошельком. cid=" + campaignId);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(campaignId)
                        .withTimestamp()
                        .withConsumeQty(qty.floatValue())
                        .withConsumeAmount(qty.floatValue())
                        .withProductCurrency((productCurrency.toString() != null) ? productCurrency.toString() : "")
                );
        Float campaignSum = api.userSteps.getDirectJooqDbSteps()
                .useShard(api.userSteps.clientFakeSteps().getUserShard(LOGIN))
                .campaignsSteps().getCampaignById(campaignId).getSum().floatValue();
        assertThat("Баланс кампании равен ожидаемому", campaignSum,
                equalTo(qty.setScale(2, RoundingMode.HALF_UP).floatValue()));
    }

    @Test
    public void notifyOrder2WithCurrencyWalletTest() {
        Money qty = Money.valueOf(100.0f, Currency.BYN);
        log.info("Вызываем метод NotifyOrder2 для белорусского клиента. Оплачиваем wallet-кампанию. cid=" + campaignId);
        Long walletCid = api.userSteps.getDirectJooqDbSteps()
                .useShard(api.userSteps.clientFakeSteps().getUserShard(LOGIN))
                .campaignsSteps().getCampaignById(campaignId).getWalletCid();
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(walletCid)
                        .withTimestamp()
                        .withConsumeQty(qty.floatValue())
                        .withConsumeAmount(qty.floatValue())
                        .withTotalConsumeQty(0f)
                        .withProductCurrency((productCurrency.toString() != null) ? productCurrency.toString() : "")
                );

        Account account = api.userSteps.financeSteps().getAccount(LOGIN);

        assertThat("Баланс кампании равен ожидаемому", account.getAmount(),
                equalTo(qty.setScale(2, RoundingMode.HALF_UP).floatValue()));
    }

    @Test
    public void notifyOrder2WithZeroAmountTest() {
        float qty = 0.0f;
        log.info("Вызываем метод NotifyOrder2 для белорусского клиента. Оплачиваем кампанию " + campaignId + ", сумма = 0");
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(campaignId)
                        .withTimestamp()
                        .withConsumeQty(qty)
                        .withProductCurrency(Currency.BYN.toString())
                );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("Баланс кампании не изменился",
                campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0.0f));
    }
}
