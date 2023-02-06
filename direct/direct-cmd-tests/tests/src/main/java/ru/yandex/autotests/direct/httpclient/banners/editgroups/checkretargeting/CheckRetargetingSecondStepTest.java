package ru.yandex.autotests.direct.httpclient.banners.editgroups.checkretargeting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.retargeting.RetargetingCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.JsonContainer;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.directapi.common.api45.RetargetingCondition;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.data.banners.EditGroupsRequestParamsSetter.setJsonGroupParamsFromBanners;
import static ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater.eval;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by shmykov on 26.01.15.
 * TESTIRT-4179
 */
@Aqua.Test
@Description("Проверка условий ретаргетинга на втором шаге редатирования")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.RETAGRETING)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
public class CheckRetargetingSecondStepTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static String clientLogin = "at-direct-b-groups-ret-2stp";
    private static String campaignId;
    private static Long bannerId;
    private static CSRFToken csrfToken;
    private static DirectResponse response;
    private static GroupsParameters requestParams;
    private static int[] retargetingIds;
    private static List<RetargetingCmdBean> expectedRetargetings;
    private static JsonContainer jsonGroups;

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(clientLogin);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        expectedRetargetings = new ArrayList<>();


        campaignId = bannersRule.getCampaignId().toString();
        bannerId = bannersRule.getBannerId();
        cmdRule.oldSteps().onPassport().authoriseAs(clientLogin, User.get(clientLogin).getPassword());
        TestEnvironment.newDbSteps().useShardForLogin(clientLogin).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(clientLogin).getClientID()));

        PropertyLoader<RetargetingCondition> loader = new PropertyLoader<>(RetargetingCondition.class);
        retargetingIds = cmdRule.apiSteps().retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(loader.getHttpBean("retWithSingleCondAndLoginAndGoals")),
                new RetargetingConditionMap(loader.getHttpBean("retWithMultipleCondAndLoginAndGoals")));

        requestParams = new GroupsParameters();
        requestParams.setBids(String.valueOf(bannerId));
        requestParams.setCid(campaignId);
        requestParams.setUlogin(clientLogin);

        PropertyLoader<JsonContainer> jsonGroupsLoader = new PropertyLoader<>(JsonContainer.class);
        jsonGroups = jsonGroupsLoader.getHttpBean("jsonGroupsWithRetargeting2");
        JsonArray jsonGroupsArray = new JsonParser().parse(jsonGroups.toString()).getAsJsonArray();
        JsonObject group = jsonGroupsArray.get(0).getAsJsonObject();
        setJsonGroupParamsFromBanners(group, bannersRule.getCurrentGroup());
        JsonObject firstRetargeting = group.get("retargetings").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject secondRetargeting = group.get("retargetings").getAsJsonArray().get(1).getAsJsonObject();
        firstRetargeting.addProperty("ret_cond_id", retargetingIds[0]);
        secondRetargeting.addProperty("ret_cond_id", retargetingIds[1]);
        requestParams.setJsonGroups(jsonGroupsArray.toString());

        expectedRetargetings.add(eval(firstRetargeting.toString(), new RetargetingCmdBean(), BeanType.REQUEST));
        expectedRetargetings.add(eval(secondRetargeting.toString(), new RetargetingCmdBean(), BeanType.REQUEST));

        csrfToken = getCsrfTokenFromCocaine(User.get(clientLogin).getPassportUID());
    }

    @Test
    @Description("Проверка условий ретартегинга при возвращении со второго шага на первый")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10085")
    public void goBackSchemaTest() {
        requestParams.setAdgroupIds(bannersRule.getGroupId().toString());
        response = cmdRule.oldSteps().groupsSteps().goBackShowCampMultiEdit(csrfToken, requestParams);
        String firstRetargeting = cmdRule.oldSteps().commonSteps().readResponseJsonProperty(
                response, "campaign..retargetings[0]").toString();
        String secondRetargeting = cmdRule.oldSteps().commonSteps().readResponseJsonProperty(
                response, "campaign..retargetings[1]").toString();

        assertThat("условие ретаретинга соответствует ожиданиям",
                eval(firstRetargeting, new RetargetingCmdBean(), BeanType.RESPONSE),
                beanEquivalent(expectedRetargetings.get(0)));
        assertThat("условие ретаретинга соответствует ожиданиям",
                eval(secondRetargeting, new RetargetingCmdBean(), BeanType.RESPONSE),
                beanEquivalent(expectedRetargetings.get(1)));
    }
}
