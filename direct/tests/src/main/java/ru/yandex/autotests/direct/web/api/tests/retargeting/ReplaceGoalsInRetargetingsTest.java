package ru.yandex.autotests.direct.web.api.tests.retargeting;

import java.util.Collections;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.ReplaceGoal;
import ru.yandex.autotests.direct.web.api.rules.RetargetingConditionRule;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.web.api.data.GoalConstants.GOALS_IDS;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Замена целей в ретаргетингах")
@Stories(TestFeatures.Retargeting.GOALS)
@Feature(TestFeatures.RETARGETING)
@Tag(TrunkTag.YES)
@Tag(Tags.RETARGETING)
public class ReplaceGoalsInRetargetingsTest {

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private RetargetingConditionRule retargetingConditionRule2 =
            new RetargetingConditionRule().withUlogin(Logins.DEFAULT_CLIENT);

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(Logins.DEFAULT_CLIENT);


    @Test
    public void canReplaceGoalsInRetargetings() {
        List<ReplaceGoal> request = Collections.singletonList(new ReplaceGoal()
                .withNewGoalId(GOALS_IDS[1])
                .withOldGoalId(GOALS_IDS[0])
        );
        boolean result =
                directRule.webApiSteps().retargetingSteps().replaceGoalsInRetargetings(request, Logins.DEFAULT_CLIENT);
        assertThat("Ответ соответствует ожиданиям", result, equalTo(true));
    }


}
