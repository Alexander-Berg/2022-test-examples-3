package ru.yandex.autotests.direct.cmd.campaigns.broadmatchgoalid;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps.extractCidFromSaveCampResponse;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение некорректного айди broad_match цели")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
public class BroadMatchGoalInvalidTest {
    protected static final long GOAL_ID_1 = 16819470l;
    private final static String CLIENT = "at-direct-backend-c";
    private Long campaignIdToRemove;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);


    @Before
    public void before() {
    }


    @Test
    @Description("Сохраняем айди цели в новой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9393")
    public void saveBroadMatchGoalIdsToNewCamp() {
        SaveCampRequest saveCampRequest = getSaveCampRequest().withUlogin(CLIENT);
        Long savedBroadMatchGoalId = GOAL_ID_1;
        saveCampRequest.setBroadMatchGoalId(String.valueOf(savedBroadMatchGoalId));
        ErrorResponse actualError = cmdRule.cmdSteps().campaignSteps()
                .postSaveNewCampInvalidData(saveCampRequest);
        assertThat("Полученный broad_match_id соотвествует ожиданиям", actualError, not(nullValue()));
    }

    @Test
    @Description("Сохраняем айди цели в существующей кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9394")
    public void saveBroadMatchGoalIds() {
        SaveCampRequest saveCampRequest = getSaveCampRequest().withUlogin(CLIENT);
        Long savedBroadMatchGoalId = GOAL_ID_1;
        Long campaignId = extractCidFromSaveCampResponse(cmdRule.cmdSteps().campaignSteps()
                .postSaveNewCamp(saveCampRequest));
        campaignIdToRemove = campaignId;

        saveCampRequest.setBroadMatchGoalId(String.valueOf(savedBroadMatchGoalId));
        saveCampRequest.setCid(String.valueOf(campaignId));
        ErrorResponse actualError = cmdRule.cmdSteps().campaignSteps().postSaveNewCampInvalidData(saveCampRequest);
        assertThat("Полученный broad_match_id соотвествует ожиданиям", actualError, not(nullValue()));
    }


    protected SaveCampRequest getSaveCampRequest() {
        SaveCampRequest request =
                BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT, SaveCampRequest.class);
        request.setBroad_match_flag("1");
        request.setBroad_match_limit("100");
        return request;
    }

    @After
    public void delete() {
        if (campaignIdToRemove != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, campaignIdToRemove);
        }
    }
}
