package ru.yandex.autotests.direct.web.api.tests.retargeting;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.MetrikaGoalWeb;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Получение списка целей для ретаргетинга")
@Stories(TestFeatures.Retargeting.GOALS)
@Features(TestFeatures.RETARGETING)
@Tag(TrunkTag.YES)
@Tag(Tags.RETARGETING)
public class GetGoalsForRetargetingTest {

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(Logins.DEFAULT_CLIENT);

    @Test
    public void canGetRetargetingGoals() {
        List<MetrikaGoalWeb> goals = directRule.webApiSteps().retargetingSteps().getGoals(null);
        assertThat("Получены цели метрики", goals, Matchers.hasSize(Matchers.greaterThan(0)));
    }
}
