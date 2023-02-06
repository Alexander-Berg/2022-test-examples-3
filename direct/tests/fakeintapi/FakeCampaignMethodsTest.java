package ru.yandex.autotests.directintapi.tests.fakeintapi;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.directapi.model.Logins.AGENCY_YE_DEFAULT;
import static ru.yandex.autotests.directapi.model.Logins.CLIENT_FREE_YE_DEFAULT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1406
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.FAKE_METHODS)
public class FakeCampaignMethodsTest {

    private static DarkSideSteps darkSideSteps;
    private static Long cid;
    private static Long cidWithAgency;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @BeforeClass
    public static void createCampaign() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        //Проставим баллы для использования апи
        int units = 31999;
        darkSideSteps.getClientFakeSteps().setAPIUnits(Logins.LOGIN_MAIN, units);

        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();

        api.as(AGENCY_YE_DEFAULT);
        cidWithAgency = api.userSteps.addDraftCampaign(CLIENT_FREE_YE_DEFAULT);
    }


    @Test
    public void getCampaignParamsTest() {
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(cid);
        assertThat("Неверный campaignId у кампании", (long) campaignFakeInfo.getCampaignID(), equalTo(cid));
        assertNotNull(campaignFakeInfo.getLogin());
    }

    //https://jira.yandex-team.ru/browse/DIRECT-27241
    @Test
    public void getAgencyLoginFromGetCampaignParamsTest() {
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(cidWithAgency);
        assertThat("Неверный agencyLogin у кампании",
                campaignFakeInfo.getAgencyLogin(), equalTo(AGENCY_YE_DEFAULT));
    }

    @Test
    public void updateCampaignParamsTest() {
        String time = "09:00:21:0" + RandomUtils.getNextInt(10);
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(cid);
        campaignFakeInfo.setSmsTime(time);
        darkSideSteps.getCampaignFakeSteps().fakeCampaignParams(campaignFakeInfo);
        assertThat("Неверный smsTime у кампании",
                darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(cid).getSmsTime(), equalTo(time));
    }

    //https://jira.yandex-team.ru/browse/DIRECT-27420
    //в DIRECT-32318 поменялась логика - теперь ошибка не возвращается
    @Test
    public void canNotUpdateDeletedCampaignParamsTest() {
        int deletedCampaignID = 8711517;
        CampaignFakeInfo campaignFakeInfo = new CampaignFakeInfo();
        campaignFakeInfo.setCampaignID(deletedCampaignID);
        campaignFakeInfo.setSum(1234f);
        darkSideSteps.getCampaignFakeSteps().fakeCampaignParams(campaignFakeInfo);
    }

    @Test
    public void fakeNotificationFromBalanceTest() {
        float sum = RandomUtils.getNextInt(10000);
        darkSideSteps.getCampaignFakeSteps().sendFakeNotificationFromBalance(cid, sum, Currency.RUB);
        CampaignFakeInfo campaignFakeInfo = darkSideSteps.getCampaignFakeSteps().fakeGetCampaignParams(cid);
        assertThat("Неверная сумма у кампании", campaignFakeInfo.getSum(), equalTo(sum));
    }

}
