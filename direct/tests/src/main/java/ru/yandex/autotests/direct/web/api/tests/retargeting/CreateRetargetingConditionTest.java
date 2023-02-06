package ru.yandex.autotests.direct.web.api.tests.retargeting;

import java.util.Collections;

import org.junit.After;
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
import ru.yandex.autotests.direct.web.api.models.RetargetingConditionActionResponse;
import ru.yandex.autotests.direct.web.api.models.RetargetingConditionWeb;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.web.api.data.RetargetingConditionWebFactory.defaultRetargetingConditionWeb;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание условия ретаргетинга")
@Stories(TestFeatures.Retargeting.CONDITIONS)
@Feature(TestFeatures.RETARGETING)
@Tag(TrunkTag.YES)
@Tag(Tags.RETARGETING)
public class CreateRetargetingConditionTest {

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(Logins.DEFAULT_CLIENT);

    private Long retCondId;

    @Test
    public void canCreateRetargetingCondition() {
        RetargetingConditionWeb request = defaultRetargetingConditionWeb();
        RetargetingConditionActionResponse result =
                directRule.webApiSteps().retargetingSteps().createRetargetingCondition(request, Logins.DEFAULT_CLIENT);

        retCondId = result.getId();
        assertThat("Ответ соответствует ожиданиям", result.getSuccess(), equalTo(true));
    }

    @After
    public void after() {
        directRule.webApiSteps().retargetingSteps()
                .deleteRetargetingCondition(Collections.singletonList(retCondId), Logins.DEFAULT_CLIENT);
    }
}
