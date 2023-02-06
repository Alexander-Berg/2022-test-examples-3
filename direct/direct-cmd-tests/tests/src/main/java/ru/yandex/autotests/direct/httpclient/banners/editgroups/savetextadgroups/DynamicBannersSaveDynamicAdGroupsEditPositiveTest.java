package ru.yandex.autotests.direct.httpclient.banners.editgroups.savetextadgroups;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.DynamicGroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions.DynamicConditionsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by f1nal
 * on 03.07.15.
 * TESTIRT-6117
 */

@Issue("TESTIRT-6117")
@Aqua.Test
@Description("Позитивная проверка сохранения после редактирования динамических баннеров через контроллер saveDynamicAdGroups")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class DynamicBannersSaveDynamicAdGroupsEditPositiveTest extends DynamicBannersSaveDynamicAdGroupsPositiveBase {

    protected void editBanner(String beanName) {
        expectedGroups = new PropertyLoader<>(DynamicGroupsCmdBean.class).getHttpBean(beanName);
        DirectResponse response = getResponseFirstDynamicGroupFromCampaign();
        DynamicGroupsCmdBean actualGroup = JsonPathJSONPopulater.eval(response.getResponseContent().asString(), new DynamicGroupsCmdBean(),
                BeanType.RESPONSE, Arrays.asList("filter_schema_performance", "filter_schema_dynamic", "static_file_hashsums"));
        expectedGroups.getGroups().get(0).setAdGroupID(actualGroup.getGroups().get(0).getAdGroupID());
        expectedGroups.getGroups().get(0).getBanners().get(0).setBannerID(actualGroup.getGroups().get(0).getBanners().get(0).getBannerID());
        assumeThat("Проверяем, что в ответе контроллера есть 1 группа ", actualGroup.getGroups().size(),
                comparesEqualTo(1));
        assumeThat("Проверяем, что в группе есть условия ", actualGroup.getGroups().get(0).getDynamicConditions().size(),
                greaterThan(0));
        List<DynamicConditionsCmdBean> actualConditions = actualGroup.getGroups().get(0).getDynamicConditions();
        for (int i = 0; i < actualConditions.size(); i++) {
            expectedGroups.getGroups().get(0).getDynamicConditions().get(i).setDynamicConditionId(actualConditions.get(i).getDynamicConditionId());
        }

        GroupsParameters requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setJsonGroups(expectedGroups.toJson());
        response = cmdRule.oldSteps().groupsSteps().saveDynamicAdGroups(csrfToken, requestParams);
        Matcher toShowCampRedirectMatcher = allOf(
                containsString("cmd=" + CMD.SHOW_CAMP),
                containsString("cid=" + campaignId));
        cmdRule.oldSteps().commonSteps().checkJsonRedirect(response, toShowCampRedirectMatcher);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10121")
    public void editDynamicBannerPositiveTest() {
        super.editDynamicBannerPositiveTest();
    }
}
