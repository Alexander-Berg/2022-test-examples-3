package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusempty;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Created by semkagtn on 29.06.15.
 * https://st.yandex-team.ru/TESTIRT-6104
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@Issue("https://st.yandex-team.ru/DIRECT-43034")
@Description("Вызов метода NotifyOrder2 для кампании, у которой StatusEmpty = Yes")
public class NotifyOrder2ForEmptyCampaignTest {

    private static final String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static long cid;

    @BeforeClass
    public static void createEmptyCampaign() {
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
    }

    @Before
    @Step("Подготовка данных для теста")
    public void setStatusEmptyNo() {
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN);
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().setStatusEmpty(cid, CampaignsStatusempty.Yes);
    }

    @Test
    @Description("Нулевая сумма в нотификации для пустой кампании")
    public void notifyOrder2WithZeroSum() {
        float sum = 0.0f;
        String expectedResponse = String.format(
                "Campaign %d is empty, skipping zero sum, but accepting notification", cid);
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(cid)
                                .withTimestamp()
                                .withConsumeQty(sum)
                                .withProductCurrency(Currency.RUB.toString()),
                        200, 0, expectedResponse
                );
    }

    @Test
    @Description("Ненулевая сумма в нотификации для пустой кампании")
    public void notifyOrder2WithNonZeroSum() {
        float sum = 1.5f;
        String sumString = notifyOrderNumberFormat(sum);
        String expectedResponse = String.format(
                "Campaign %d is empty, can't update sum: %s", cid, sumString);
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(cid)
                                .withTimestamp()
                                .withConsumeQty(sum)
                                .withProductCurrency(Currency.RUB.toString()),
                        400, 4010, expectedResponse
                );
    }

    @After
    public void prepareCampaignForDelete() {
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().setStatusEmpty(cid, CampaignsStatusempty.No);
    }

    private static String notifyOrderNumberFormat(double num) {
        DecimalFormatSymbols dSymbols = DecimalFormatSymbols.getInstance();
        dSymbols.setDecimalSeparator('.');
        return new DecimalFormat("0.0000000", dSymbols).format(num);
    }
}
