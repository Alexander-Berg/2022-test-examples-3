package ru.yandex.autotests.direct.httpclient.banners.editgroups.checkretargeting;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.directapi.common.api45.RetargetingCondition;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;

@Aqua.Test
@Description("Проверка условий ретаргетинга на первом шаге редактирования")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.RETAGRETING)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
public class CheckRetargetingFirstStepTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String clientLogin = "at-direct-b-groups-ret";
    protected static Long campaignId;
    protected static Long bannerId;
    protected static CSRFToken csrfToken;
    protected static DirectResponse response;
    protected static GroupsParameters requestParams;
    protected List<RetargetingCondition> retargetings;
    protected List<String> retargetingIds;
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(clientLogin);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {

        campaignId = bannersRule.getCampaignId();
        bannerId = bannersRule.getBannerId();
        cmdRule.oldSteps().onPassport().authoriseAs(clientLogin, User.get(clientLogin).getPassword());
        TestEnvironment.newDbSteps().useShardForLogin(clientLogin).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(clientLogin).getClientID()));

        cmdRule.getApiStepsRule().as(clientLogin);
        retargetings = cmdRule.apiSteps().retargetingSteps()
                .addRandomConditionsForUserBanner(clientLogin, bannerId, 2);

        csrfToken = getCsrfTokenFromCocaine(User.get(clientLogin).getPassportUID());

        requestParams = new GroupsParameters();
        requestParams.setBids(String.valueOf(bannerId));
        requestParams.setCid(campaignId.toString());
        requestParams.setUlogin(clientLogin);
    }

    @Test
    @Description("Проверка id ретартегинга в ответе контроллера showCampMultiEdit")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10080")
    public void checkRetargetingInShowCampMultiEditTest() {
        retargetingIds = retargetings.stream()
                .map(RetargetingCondition::getRetargetingConditionID)
                .map(String::valueOf)
                .collect(toList());

        requestParams.setAdgroupIds(String.valueOf(bannersRule.getGroupId()));
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response,
                hasJsonProperty("$.campaign.groups[0].retargetings[*].ret_cond_id",
                        containsInAnyOrder(retargetingIds.toArray())));
    }

    @Test
    @Description("Проверка id ретартегинга в ответе контроллера showCampMultiEdit в поле all-retargeting_conditions")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10079")
    public void checkAllRetargetingConditionInShowCampMultiEditTest() {
        retargetingIds = new ArrayList<>();
        for (int retId : cmdRule.apiSteps().retargetingSteps().getRetargetingConditions(clientLogin)) {
            retargetingIds.add(String.valueOf(retId));
        }

        requestParams.setAdgroupIds(String.valueOf(bannersRule.getGroupId()));
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response,
                hasJsonProperty("$.all_retargeting_conditions.*.ret_cond_id",
                        containsInAnyOrder(retargetingIds.toArray())));
    }
}
