package ru.yandex.autotests.direct.httpclient.banners.editgroups.showcampmultiedit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.groups.EditGroupsErrors;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@Aqua.Test
@Description("Вызов showCampMultiEdit с неверными параметрами запроса")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
public class WrongRequestParametersShowCampMultiEditTest {
    private String clientLogin = "at-direct-b-showcampmultiedit";
    private String otherClientLogin = "at-backend-banners";
    public BannersRule bannersRule1 = new TextBannersRule().withUlogin(clientLogin);
    public BannersRule bannersRule2 = new TextBannersRule().withUlogin(otherClientLogin);
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2);
    private Integer campaignId;
    private DirectResponse response;
    private GroupsParameters requestParams;

    @Before
    public void before() {


        campaignId = bannersRule1.getCampaignId().intValue();

        requestParams = new GroupsParameters();
        requestParams.setUlogin(clientLogin);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setAdgroupIds(String.valueOf(bannersRule1.getGroupId()));
        cmdRule.oldSteps().onPassport().authoriseAs(clientLogin, User.get(clientLogin).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(clientLogin).getPassportUID());
    }

    @Test
    @Description("Вызов без adgroup_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10155")
    public void noAdGroupTest() {
        requestParams.setAdgroupIds(null);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, startsWith(EditGroupsErrors.NO_GROUP_NUMBER.toString()));
    }

    @Test
    @Description("Вызов c чужим adgroup_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10154")
    public void wrongAdGroupTest() {
        requestParams.setAdgroupIds(String.valueOf(bannersRule2.getGroupId()));
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Вызов c неверным cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10153")
    public void wrongCidTest() {
        requestParams.setCid("123");
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
