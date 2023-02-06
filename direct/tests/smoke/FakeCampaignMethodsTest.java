package ru.yandex.autotests.directintapi.tests.smoke;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1406
 * https://jira.yandex-team.ru/browse/DIRECT-27241
 */

@Aqua.Test
@Features(FeatureNames.FAKE_INTAPI_MONITOR)
public class FakeCampaignMethodsTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    public static int CAMPAIGN_ID = 10437729;

    @Rule
    public BottleMessageRule bmr = new BottleMessageRule();

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Stories("FakeGetCampaignParams")
    @Test
    public void getCampaignParamsTest() {
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(CAMPAIGN_ID);
        assertThat("Неверный campaignId у кампании", campaignFakeInfo.getCampaignID(), equalTo(CAMPAIGN_ID));
        assertNotNull(campaignFakeInfo.getLogin());
    }

    @Stories("FakeCampaignParams")
    @Test
    public void updateCampaignParamsTest() {
        String time = "09:00:21:0" + RandomUtils.getNextInt(10);
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(CAMPAIGN_ID);
        campaignFakeInfo.setSmsTime(time);
        darkSideSteps.getCampaignFakeSteps().fakeCampaignParams(campaignFakeInfo);
        assertThat("Неверный smsTime у кампании",
                darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(CAMPAIGN_ID).getSmsTime(), equalTo(time));
    }

    @Stories("FakeBalanceNotification")
    @Test
    public void fakeNotificationFromBalanceTest() {
        float sum = RandomUtils.getNextInt(10000);
        darkSideSteps.getCampaignFakeSteps().sendFakeNotificationFromBalance(CAMPAIGN_ID, sum, Currency.YND_FIXED);
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(CAMPAIGN_ID);
        assertThat("Неверная сумма у кампании", campaignFakeInfo.getSum(), equalTo(sum));
    }

}