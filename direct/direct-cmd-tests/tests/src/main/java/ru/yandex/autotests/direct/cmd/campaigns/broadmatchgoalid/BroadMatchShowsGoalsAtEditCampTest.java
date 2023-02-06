package ru.yandex.autotests.direct.cmd.campaigns.broadmatchgoalid;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.Goal;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Получение доступных целей контроллером editCamp")
@Stories(TestFeatures.Campaigns.EDIT_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.EDIT_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class BroadMatchShowsGoalsAtEditCampTest extends BroadMatchGoalValidBaseTest {

    public BroadMatchShowsGoalsAtEditCampTest(String broadMatchGoalId) {
        super(broadMatchGoalId, CampaignTypeEnum.TEXT);
    }

    @Parameterized.Parameters(name = "id цели : {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {String.valueOf(GOAL_ID_1)},
                {String.valueOf(GOAL_ID_2)},
                {String.valueOf(GOAL_ID_3)}, // цель с parent_goal_id
        });
    }

    @Test
    @Description("Проверяем отображение цели контроллером editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9397")
    public void goalIdsAtEditCamp() {
        EditCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        assumeThat("список целей вернулся", actualResponse.getGoalsList(), notNullValue());

        assertThat("Цель содержится в списке доступных целей", actualResponse.getGoalsList().stream()
                        .map(Goal::getGoalId)
                        .collect(Collectors.toList()),
                hasItem(broadMatchGoalId));
    }
}
