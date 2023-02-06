package ru.yandex.autotests.direct.httpclient.banners;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.ContactInfoCmdBean;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.banners.managevcards.ManageVCardsRequestParams;
import ru.yandex.autotests.direct.httpclient.data.banners.managevcards.ManageVCardsResponseBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.direct.httpclient.util.mappers.ContactInfoApiToCmd.ContactInfoApiToCmdBeanMappingBuilder;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by shmykov on 12.06.15.
 * TESTIRT-4997
 */
@Aqua.Test
@Description("Проверки контоллера ManageVCards(Страница Мастер визиток)")
@Stories(TestFeatures.Banners.MANAGE_VCARDS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.VCARD)
@Tag(CmdTag.MANAGE_VCARDS)
@Tag(OldTag.YES)
public class ManageVcardsTest {
    private String CLIENT = "at-direct-b-managevcards";
    private Long campaignId;
    private ManageVCardsRequestParams requestParams;
    private DirectResponse response;
    private static CSRFToken csrfToken;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    TextBannersRule bannersRule = new TextBannersRule().overrideBannerTemplate(new Banner()
            .withHasVcard(1)
            .withContactInfo(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class)))
            .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        cmdRule.apiAggregationSteps().getAllCampaigns(CLIENT);
        campaignId = bannersRule.getCampaignId();
        requestParams = new ManageVCardsRequestParams();
        requestParams.setCid(String.valueOf(campaignId));
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
    }

    @Test
    @Description("Проверка визиток на странице мастера визиток")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10069")
    public void vcardsOnPageTest() {
        ContactInfoCmdBean expectedVcard = BeanMapper.map(
                bannersRule.getCurrentGroup().getBanners().get(0).getContactInfo(),
                ContactInfoCmdBean.class,
                new ContactInfoApiToCmdBeanMappingBuilder());
        response = cmdRule.oldSteps().manageVCardsSteps().openManageVCards(requestParams, csrfToken);
        ManageVCardsResponseBean responseBean = JsonPathJSONPopulater.evaluateResponse(
                response, new ManageVCardsResponseBean());
        assertThat("визитки в ответе контроллера соответствуют ожиданиям", responseBean.getVcards(),
                hasItem(beanEquivalent(expectedVcard)));

    }

    @Test
    @Description("Вызов с неверным cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10071")
    public void wrongCidTest() {
        requestParams.setCid("123");
        response = cmdRule.oldSteps().manageVCardsSteps().openManageVCards(requestParams, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Вызов с неверным login")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10070")
    public void wrongUloginTest() {
        requestParams.setUlogin(Logins.SUPER);
        response = cmdRule.oldSteps().manageVCardsSteps().openManageVCards(requestParams, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
