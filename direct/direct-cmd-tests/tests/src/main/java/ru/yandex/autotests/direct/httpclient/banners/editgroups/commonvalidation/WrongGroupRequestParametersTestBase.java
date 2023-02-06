package ru.yandex.autotests.direct.httpclient.banners.editgroups.commonvalidation;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;

/**
 * Created by shmykov on 06.05.15.
 * TESTIRT-4957
 */
public abstract class WrongGroupRequestParametersTestBase {

    protected static final String CLIENT_LOGIN = "at-direct-b-bannersmultiedit";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;

    protected TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    
    protected String otherClientLogin = "at-backend-banners";
    protected Long otherCampaignId;
    protected DirectResponse response;
    protected GroupsParameters requestParams;
    protected GroupsCmdBean expectedGroups;

    @Before
    public void before() {
        expectedGroups = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("singleGroupForBannersMultiSave2");
        expectedGroups.getGroups().get(0).setAdGroupID(bannersRule.getGroupId().toString());
        expectedGroups.getGroups().get(0).getBanners().get(0).setBannerID(String.valueOf(bannersRule.getBannerId()));
        requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(bannersRule.getCampaignId()));
        requestParams.setJsonGroups(expectedGroups.toJson());
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_LOGIN));
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
    }

    @Description("Вызов c неверным cid")
    public void wrongCidTest() {
        requestParams.setCid("123");
        doRequest();
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Description("Вызов без cid")
    public void withoutCidTest() {
        requestParams.setCid(null);
        doRequest();
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CampaignValidationErrors.EMPTY_CID.toString());
    }

    protected abstract void doRequest();

    @After
    public void deleteCampaign() {
        if (otherCampaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(otherClientLogin, otherCampaignId);
        }
    }
}
