package ru.yandex.autotests.direct.cmd.campaigns.broadmatchgoalid;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение корректного айди broad_match цели")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class BroadMatchGoalValidTest extends BroadMatchGoalValidBaseTest {

    private String expectedBroadMatchGoalId;

    public BroadMatchGoalValidTest(String broadMatchGoalId,
                                   String expectedBroadMatchGoalId,
                                   CampaignTypeEnum campaignType) {
        super(broadMatchGoalId, campaignType);
        this.expectedBroadMatchGoalId = expectedBroadMatchGoalId;
    }

    @Parameterized.Parameters(name = "id цели : {0} в кампании {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"16819470", "16819470", CampaignTypeEnum.MOBILE},
                {"16819470", "16819470", CampaignTypeEnum.TEXT},
                {String.valueOf(GOAL_ID_3), String.valueOf(GOAL_ID_3), CampaignTypeEnum.TEXT}, // цель с parent_goal_id
                {"", null, CampaignTypeEnum.MOBILE},
                {"", null, CampaignTypeEnum.TEXT},
                {"0", "0", CampaignTypeEnum.MOBILE},
                {"0", "0", CampaignTypeEnum.TEXT},
        });
    }

    @Test
    @Description("Сохраняем айди цели")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9395")
    public void saveBroadMatchGoalIds() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest().withMobileAppId(null);
        saveCampRequest.setCid(String.valueOf(bannersRule.getCampaignId()));
        saveCampRequest.setBroadMatchGoalId(broadMatchGoalId);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        String broadMatchGoalId = cmdRule.cmdSteps().campaignSteps().
                getShowCamp(CLIENT, String.valueOf(bannersRule.getCampaignId())).getBroadMatchGoalId();
        assertThat("Полученный broad_match_id соотвествует ожиданиям", broadMatchGoalId,
                equalTo(expectedBroadMatchGoalId));
    }

}
