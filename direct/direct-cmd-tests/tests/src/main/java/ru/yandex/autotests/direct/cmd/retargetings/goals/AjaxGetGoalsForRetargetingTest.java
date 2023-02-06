package ru.yandex.autotests.direct.cmd.retargetings.goals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxGetGoalsForRetargetingItem;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Просмотр списка целей пользователя (cmd = ajaxGetGoalsForRetargeting)")
@Stories(TestFeatures.Retargeting.AJAX_GET_GOALS_FOR_RETARGETING)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_GET_GOALS_FOR_RETARGETING)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxGetGoalsForRetargetingTest {

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;

    private String client;
    private Long[] expectedGoalIds;

    @SuppressWarnings("unused")
    public AjaxGetGoalsForRetargetingTest(String client, Long[] expectedGoalIds) {
        this.client = client;
        this.expectedGoalIds = expectedGoalIds;
        this.cmdRule = DirectCmdRule.defaultRule().as(client);
    }

    @Parameterized.Parameters(name = "Клиент: {0}; ожидаемый список целей: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"at-direct-b-getgoals-onegoal", new Long[]{4028110402L}},
                {"at-direct-b-getgoals-manygoals", new Long[]{4028110999L, 4028110924L, 7378824L}},
                {"at-direct-b-getgoals-nogoals", new Long[0]},
        });
    }

    @Test
    @Description("Просмотр списка целей пользователя (cmd = ajaxGetGoalsForRetargeting)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9954")
    public void testAjaxGetGoalsForRetargeting() {
        List<AjaxGetGoalsForRetargetingItem> goals =
                cmdRule.cmdSteps().retargetingSteps().getAjaxGetGoalsForRetargeting(client);
        List<Long> actualGoalIds = goals.
                stream().
                map(AjaxGetGoalsForRetargetingItem::getGoalId).
                collect(Collectors.toList());
        assertThat("список целей соответствует ожидаемому", actualGoalIds, containsInAnyOrder(expectedGoalIds));
    }
}
