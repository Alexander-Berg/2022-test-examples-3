package ru.yandex.autotests.direct.cmd.retargetings.save;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingConditionItem;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.RetConditionErrorsResource;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Недоступность сохранения условия ретаргетинга с аудиторией под клиентом, " +
        "у которого нет условий с аудиториями (cmd = ajaxSaveRetargetingCond)")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_SAVE_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
@Ignore
public class AjaxSaveRetargetingCondAudienceUnavailableTest {

    private static final String CLIENT = "at-retargeting-invalidparams2";
    private static final String AUDIENCE = "2000000122";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);


    private RetargetingCondition retCondition;

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));

        retCondition = new RetargetingCondition()
                .withConditionName("Invalid Cond")
                .withConditionDesc("desc")
                .withCondition(singletonList(
                        new RetargetingConditionItem()
                                .withType(RetConditionItemType.ALL.getValue())
                                .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCE)))
                        )
                );

    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с пустым именем (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9962")
    public void testAddAtAjaxSaveRetargetingCondEmptyName() {
        AjaxSaveRetargetingCondResponse actualResponse =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, CLIENT);
        assertThat("ответ соответствует ожидаемому", actualResponse.getErrorText(),
                equalTo(RetConditionErrorsResource.AUDIENCE_UNAVAILABLE.getErrorText()));
    }
}
