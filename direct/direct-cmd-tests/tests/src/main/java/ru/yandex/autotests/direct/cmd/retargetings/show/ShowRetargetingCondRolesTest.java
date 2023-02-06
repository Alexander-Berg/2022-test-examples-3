package ru.yandex.autotests.direct.cmd.retargetings.show;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@Aqua.Test
@Description("Просмотр условий ретаргетинга под разными ролями (cmd = showRetargetingCond)")
@Stories(TestFeatures.Retargeting.SHOW_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.SHOW_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ShowRetargetingCondRolesTest {

    private static final String RET_COND_TEMPLATE = "cmd.common.request.retargetingCondition.AjaxSaveRetargetingCondTest";
    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;

    private String client;
    private String lookingUser;
    private RetargetingCondition expectedRetCondition;
    private Long retCondId;

    @SuppressWarnings("unused")
    public ShowRetargetingCondRolesTest(String client, String lookingUser, String role) {
        this.client = client;
        this.lookingUser = lookingUser;
        this.cmdRule = DirectCmdRule.defaultRule().as(client);
    }

    @Parameterized.Parameters(name = "Клиент: {0}; просматриваем под: {1} ({2})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"at-direct-b-withmngr-ret", Logins.MANAGER, "менеджер"},
                {"at-direct-b-with-ag-ret", Logins.AGENCY, "агентство"},
                {"at-direct-b-withplacer-ret", Logins.PLACER, "вешальщик"},
                {"at-direct-b-withmedia-ret", Logins.MEDIAPLANER, "медиапланер"},
                {"at-direct-b-withsup-ret", Logins.SUPER, "супер"},
        });
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(client).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(client).getClientID()));

        expectedRetCondition = BeanLoadHelper.loadCmdBean(RET_COND_TEMPLATE, RetargetingCondition.class);
        retCondId = cmdRule.cmdSteps().retargetingSteps().saveRetargetingConditionWithAssumption(expectedRetCondition, client);
        expectedRetCondition.withRetCondId(retCondId).withIsAccessible(1);

        cmdRule.cmdSteps().authSteps().authenticate(User.get(lookingUser));
    }

    @Test
    @Description("Просмотр условий ретаргетинга под разными ролями (cmd = showRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9988")
    public void testShowRetargetingCondRoles() {
        Map<Long, RetargetingCondition> actualRetConditions =
                cmdRule.cmdSteps().retargetingSteps().getShowRetargetingCond(client);

        Map<Long, RetargetingCondition> expectedRetConditions = new HashMap<>();
        expectedRetConditions.put(retCondId, expectedRetCondition);

        assertThat("получен правильный список ретаргетингов", actualRetConditions,
                beanDiffer(expectedRetConditions).useCompareStrategy(
                        onlyFields(newPath("goal_id"), newPath("goal_type"), newPath("time"))));
    }
}
