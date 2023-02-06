package ru.yandex.autotests.directintapi.tests.ppcretargetingcheckgoals;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.tags.StageTag;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory.defaultInterests;

@Aqua.Test
@Tag(StageTag.RELEASE)
@Features(FeatureNames.PPC_RETARGETING_CHECK_GOALS)
@Issue("https://st.yandex-team.ru/DIRECT-43212")
@Description("Вызов скрипта ppcRetargetingCheckGoals.pm для интереса")
public class PpcRetargetingCheckGoalsInterestTest {
    private static final String CLIENT = "at-direct-backend-c";

    private int shard;
    private String clientId;

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule;
    @Rule
    public DirectCmdRule cmdRule;
    private Long categoryId;
    private Long retId;

    public PpcRetargetingCheckGoalsInterestTest() {
        categoryId = RetargetingHelper.createTargetCategory(CLIENT, null).getCategoryId();
        bannersRule = new MobileBannersRule()
                .overrideGroupTemplate(new Group().withTargetInterests(defaultInterests(categoryId)))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        retId = TestEnvironment.newDbSteps().bidsSteps()
                .getBidsRetargetingRecordByPid(bannersRule.getGroupId()).get(0).getRetId();

        clientId = User.get(CLIENT).getClientID();
        shard = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).getCurrentPpcShard();

        String expectedPriceContext = "0.88";
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withInterest(String.valueOf(bannersRule.getGroupId()), new AjaxUpdateShowConditionsObjects()
                        .withEdited(
                                String.valueOf(retId),
                                new AjaxUpdateShowConditions().withPriceContext(expectedPriceContext)
                        ))
                .withUlogin(CLIENT);
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);

    }

    @Test
    public void runScriptAndCheckResult() {
        cmdRule.apiSteps().getDarkSideSteps().getRunScriptSteps().runPpcRetargetingCheckGoals(shard, clientId);
    }

    @After
    public void after() {
        if (categoryId != null) {
            TestEnvironment.newDbSteps().interestSteps()
                    .deleteTargetingCategoriesRecords(categoryId);
        }
    }
}
