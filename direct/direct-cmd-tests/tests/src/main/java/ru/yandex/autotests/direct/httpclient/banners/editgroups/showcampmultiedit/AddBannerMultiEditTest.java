package ru.yandex.autotests.direct.httpclient.banners.editgroups.showcampmultiedit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource.NOT_USUAL_INTERFACE_REQUEST;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@Aqua.Test
@Description("Тесты контроллера addBannerMultiEdit")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class AddBannerMultiEditTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static CSRFToken csrfToken;
    private String clientLogin = "at-direct-b-addbannermultiedit";

    private Long campaignId;
    private DirectResponse response;
    private GroupsParameters requestParams;
    private GroupsCmdBean expectedGroups;

    public BannersRule bannersRule = new TextBannersRule().withUlogin(clientLogin);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        requestParams = new GroupsParameters();
        requestParams.setUlogin(clientLogin);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setFromNewCamp("1");
        cmdRule.oldSteps().onPassport().authoriseAs(clientLogin, User.get(clientLogin).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(clientLogin).getPassportUID());

    }

    @Test
    @Description("Проверка ответа контроллера addBannerMultiEdit")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10136")
    public void addBannerMultiEditResponseTest() {
        requestParams.setNewGroup("1");
        response = cmdRule.oldSteps().groupsSteps().addBannerMultiEdit(csrfToken, requestParams);
        expectedGroups = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("addBannerMultiEditGroup2");
        GroupsCmdBean actualResponse = JsonPathJSONPopulater.eval(response.getResponseContent().asString(), new GroupsCmdBean(), BeanType.RESPONSE);
        assertThat("баннер в ответе контроллера соответствует сохраненному черeз апи", actualResponse, beanEquivalent(expectedGroups));
    }

    @Test
    @Description("Проверка ошибки в ответе контроллера addBannerMultiEdit при отсутствии параметра from_new_camp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10137")
    public void fromNewCampParameterAbsenceTest() {
        requestParams.setFromNewCamp(null);
        response = cmdRule.oldSteps().groupsSteps().addBannerMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(NOT_USUAL_INTERFACE_REQUEST.toString()));
    }

    @Test
    @Description("Проверка отсутствия групп в ответе контроллера addBannerMultiEdit при вызове с параметром adgroup_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10138")
    public void addBannerMultiEditWithAdgroupIdTest() {
        requestParams.setAdgroupIds(String.valueOf(bannersRule.getGroupId()));
        response = cmdRule.oldSteps().groupsSteps().addBannerMultiEdit(csrfToken, requestParams);
        GroupsCmdBean actualResponse = JsonPathJSONPopulater.eval(response.getResponseContent().asString(), new GroupsCmdBean(), BeanType.RESPONSE);
        assertThat("в ответе контроллера отсутствует id группы, переданный в запросе", actualResponse.getGroups().get(0).getAdGroupID(), equalTo(null));
    }
}
