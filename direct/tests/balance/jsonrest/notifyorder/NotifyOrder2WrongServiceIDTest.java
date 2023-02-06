package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: pavryabov
 * Date: 19.03.13
 * https://st.yandex-team.ru/TESTIRT-8717
 * https://st.yandex-team.ru/TESTIRT-340
 */
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-38456"),
})
@Aqua.Test(title = "NotifyOrder2 - неправильный ServiceID")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@RunWith(Parameterized.class)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2WrongServiceIDTest {

    public static final int INVALID_SERVICE_ID = 0;
    public static final String ERROR_TEXT_FORMAT = "Campaign %d with type %s does not correspond to service %s";

    private static DarkSideSteps darkSideSteps;
    private static long campaignId;
    private static long bayanCampaignID;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_YNDX_FIXED);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public int serviceID;
    @Parameterized.Parameter(1)
    public int errorCode;
    @Parameterized.Parameter(2)
    public String errorText;

    @Parameterized.Parameters(name = "ServiceId = {0}, errorText = {1}")
    public static Collection testData() {
        return Arrays.asList(new Object[][]{
                {INVALID_SERVICE_ID, 1008, "invalid service " + INVALID_SERVICE_ID},
                {NotifyOrder2JSONRequest.DIRECT_SERVICE_ID, 1014, ERROR_TEXT_FORMAT},
                {NotifyOrder2JSONRequest.BAYAN_SERVICE_ID, 1014, ERROR_TEXT_FORMAT}
        });
    }

    @BeforeClass
    public static void init() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        bayanCampaignID = api.userSteps.campaignSteps().addDefaultTextCampaign();
        darkSideSteps.getCampaignFakeSteps().setType(bayanCampaignID, CampaignsType.MCB);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_YNDX_FIXED);
    }


    @Test
    @Description("Вызываем NotifyOrder2 с неправильным ServiceID: " +
            "1. Вызываем с невалидным serviceId; 2. Вызываем с баяновским serviceId для текстовой кампании; " +
            "3. Вызываем с директовским serviceId для МКБ. " +
            "Проверяем, что вызов вернул ошибку и текст ошибки соответствует переданным параметрам")
    public void notifyOrder2WithWrongServiceIDTest() {
        AllureUtils.changeTestCaseTitle("Вызов NotifyOrder2 с неправильным ServiceID = " + serviceID);
        float qty = 100.0f;
        long cid = serviceID == NotifyOrder2JSONRequest.DIRECT_SERVICE_ID ? bayanCampaignID : campaignId;
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(cid);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(serviceID)
                        .withServiceOrderId(cid)
                        .withTimestamp()
                        .withConsumeQty(qty),
                400, errorCode, format(errorText, cid, campaignFakeInfo.getType(), serviceID)
        );

        CampaignsRecord campaigns = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(cid);
        assertThat("Баланс кампании не изменился", campaigns.getSum(), equalTo(BigDecimal.ZERO.setScale(6)));
    }
}
