package ru.yandex.autotests.directmonitoring.tests.campaign;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectMonitoringTest;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.autotests.directweb.data.textresources.ShowCampaignsResource;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.directweb.util.OperationSystemUtils.sleep;
import static ru.yandex.autotests.directweb.util.matchers.IsDisplayedMatcher.isDisplayed;


/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 24.03.15
 *         https://st.yandex-team.ru/TESTIRT-4781
 */

@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.CAMPAIGN)
@Title("Удаление кампании")
public class CampaignDeleteTest extends BaseDirectMonitoringTest {

    private static final Integer WAIT_TIME = 7500;

    private String campaignID;
    private ApiSteps apiSteps;

    @Override
    public void additionalActions() {}

    @Before
    public void before() {
        apiSteps = new ApiSteps().as(CLIENT_LOGIN);
        campaignID = String.valueOf(apiSteps.userSteps.createDraftCampaign());
        user.inCaptchaForm().fillCaptcha();
    }

    @Test
    @Title("Проверка удаления компании")
    public void campaignDelete(){
        sleep(WAIT_TIME);
        user.inBrowserAddressBar().openShowCampsPage();
        user.onShowCampsPage().selectCampaignCheckbox(campaignID);
        user.onShowCampsPage().performMassActionWithAlert(ShowCampaignsResource.DELETE_ACTION, not(equalTo("")));
        user.onShowCampsPage().shouldSeeCampaign(campaignID, not(isDisplayed()));
    }

    @After
    public void deleteCampaign() {
        if (campaignID != null) {
            Integer[] campaigns = ArrayUtils.toObject(apiSteps.userSteps.campaignSteps().getCampaignIDs(CLIENT_LOGIN));
            if (hasItemInArray(Integer.valueOf(campaignID)).matches(campaigns)) {
                apiSteps.userSteps.campaignSteps().deleteCampaignsQuietly(Integer.valueOf(campaignID));
            }
        }
    }

}
