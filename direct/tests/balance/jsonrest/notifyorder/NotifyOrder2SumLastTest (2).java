package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by ginger on 19.10.15.
 * https://st.yandex-team.ru/TESTIRT-7191
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-47408"),
        @Issue("https://st.yandex-team.ru/DIRECT-46230")
})
@Description("проверка после NotifyOrder2 изменения sum_last")
@RunWith(Parameterized.class)
public class NotifyOrder2SumLastTest {

    private static DarkSideSteps darkSideSteps;
    private static long directCampaignID;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String login;

    @Parameterized.Parameter(value = 2)
    public String currency;

    @Parameterized.Parameter(value = 3)
    public Float firstQty;

    @Parameterized.Parameter(value = 4)
    public Float secondPayment;

    @Parameterized.Parameter(value = 5)
    public Float expectedSumLast;

    @Parameterized.Parameters(name = "test = {0}")
    public static Collection dataSet() {
        Object[][] data = new Object[][]{
                {"RUB: платёж меньше минимального считаемого платежом", Logins.LOGIN_MAIN, Currency.RUB.value(), 2000f,
                        MoneyCurrency.get(Currency.RUB).getMinInvoiceInterpretation().getPrevious().floatValue(),
                        2000f},
                {"RUB: платёж меньше минимального", Logins.LOGIN_MAIN, Currency.RUB.value(), 2000f,
                        MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().getPrevious().floatValue(),
                        2000f},
                {"RUB: платёж равен минимальному", Logins.LOGIN_MAIN, Currency.RUB.value(), 2000f,
                        MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().floatValue(),
                        MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().floatValue()},
                {"RUB: платёж больше минимального", Logins.LOGIN_MAIN, Currency.RUB.value(), 2000f,
                        MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().getNext().floatValue(),
                        MoneyCurrency.get(Currency.RUB).getMinInvoiceAmount().getNext().floatValue()}
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void init() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
    }

    @Before
    public void createCampaign() {
        api.as(login);
        directCampaignID = api.userSteps.campaignSteps().addDefaultTextCampaign();
    }

    @Test
    public void checkSumLastAfterPayment() {
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(directCampaignID)
                .withTimestamp()
                .withConsumeQty(firstQty)
                .withProductCurrency(currency)
        );

        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(directCampaignID);
        assumeThat("sum поменялся верно", campaignFakeInfo.getSum(), equalTo(firstQty));
        assumeThat("sum_last поменялся верно", campaignFakeInfo.getSumLast(), equalTo(firstQty));
        float secondQty = firstQty + secondPayment;
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(directCampaignID)
                .withTimestamp()
                .withConsumeQty(secondQty)
                .withProductCurrency(currency)
        );
        campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(directCampaignID);
        assumeThat("sum поменялся верно", campaignFakeInfo.getSum(), equalTo(secondQty));
        assertThat("sum_last поменялся верно", campaignFakeInfo.getSumLast(), equalTo(expectedSumLast));
    }
}
