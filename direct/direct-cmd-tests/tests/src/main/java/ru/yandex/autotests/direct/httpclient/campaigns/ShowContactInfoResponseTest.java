package ru.yandex.autotests.direct.httpclient.campaigns;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.ContactInfoCmdBean;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.campaigns.showcontactinfo.ShowContactInfoParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.showcontactinfo.ShowContactInfoResponseBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.BeanLoadHelper.loadCmdBean;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by shmykov on 16.06.15.
 * TESTIRT-5024
 */
@Aqua.Test
@Description("Тесты контроллера showContactInfoTest(попап визитки)")
@Stories(TestFeatures.Campaigns.SHOW_CONTACT_INFO)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.VCARD)
@Tag(CmdTag.SHOW_CONTACT_INFO)
@Tag(OldTag.YES)
public class ShowContactInfoResponseTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static CSRFToken csrfToken;
    private final String CLIENT_LOGIN = "at-direct-b-showvcard";
    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideVCardTemplate(loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class))
            .withUlogin(CLIENT_LOGIN);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private DirectResponse response;
    private ShowContactInfoParameters requestParams;

    @Before
    public void before() {
        requestParams = new ShowContactInfoParameters();
        requestParams.setCid(String.valueOf(bannersRule.getCampaignId()));
        requestParams.setBid(String.valueOf(bannersRule.getBannerId()));

        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
    }

    @Test
    @Description("Проверка ответа контроллера ShowContactInfo")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10297")
    public void ShowContactInfoResponseTest() {
        ContactInfo contactInfo = bannersRule.getCurrentGroup().getBanners().get(0).getContactInfo();
        ContactInfoCmdBean expectedContactInfo = new PropertyLoader<>(ContactInfoCmdBean.class)
                .getHttpBean("expectedVcardForShowContactInfo");
        response = cmdRule.oldSteps().showContactInfoSteps().getShowContactInfo(requestParams, csrfToken);
        ShowContactInfoResponseBean controllerResponse =
                JsonPathJSONPopulater.evaluateResponse(response, new ShowContactInfoResponseBean());
        assertThat("контактная информация в ответе контроллера соответствует ожиданиям",
                controllerResponse.getContactInfo(), beanEquivalent(expectedContactInfo));
    }

    @Test
    @Description("Вызов с неверным cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10296")
    public void WrongCidTest() {
        requestParams.setCid("123");
        response = cmdRule.oldSteps().showContactInfoSteps().getShowContactInfo(requestParams, csrfToken);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Вызов с неверным bid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10298")
    public void WrongBidTest() {
        requestParams.setBid("123");
        response = cmdRule.oldSteps().showContactInfoSteps().getShowContactInfo(requestParams, csrfToken);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Вызов с неверным ulogin")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10299")
    public void WrongUloginTest() {
        requestParams.setUlogin(Logins.SUPER);
        response = cmdRule.oldSteps().showContactInfoSteps().getShowContactInfo(requestParams, csrfToken);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
