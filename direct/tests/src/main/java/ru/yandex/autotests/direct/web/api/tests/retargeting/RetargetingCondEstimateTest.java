package ru.yandex.autotests.direct.web.api.tests.retargeting;

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
import ru.yandex.autotests.direct.web.api.models.RetargetingConditionWeb;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.web.api.data.RetargetingConditionWebFactory.defaultRetargetingConditionWeb;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Прогнозируем число посетителей с указанными условием ретаргетинга")
@Stories(TestFeatures.Retargeting.GOALS)
@Feature(TestFeatures.RETARGETING)
@Tag(TrunkTag.YES)
@Tag(Tags.RETARGETING)
public class RetargetingCondEstimateTest {
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(Logins.DEFAULT_CLIENT);

    @Test
    public void canReplaceGoalsInRetargetings() {
        RetargetingConditionWeb request = defaultRetargetingConditionWeb();

        boolean result =
                directRule.webApiSteps().retargetingSteps().retargetingCondEstimate(request, Logins.DEFAULT_CLIENT);
        assertThat("Ответ соответствует ожиданиям", result, equalTo(true));
    }
}
