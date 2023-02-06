package ru.yandex.autotests.direct.httpclient.campaigns.copyCamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.campaigns.copyCamp.CopyCampRequestBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CopyCampErrors;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 24.04.15
 *         https://st.yandex-team.ru/testirt-4984
 */

@Aqua.Test
@Description("Проверка валидации при копировании кампании контроллером copyCamp")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(OldTag.YES)
public class CopyCampValidationTest {

    private static final String MANAGER = Logins.MANAGER;
    private static final String CLIENT = "at-direct-b-copycamp";
    private static final String NOT_CLIENT = "at-direct-bannerparam-c2";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    private BannersRule bannersRule2 = new TextBannersRule().withUlogin(CLIENT);
    private CampaignRule campaignRule = new CampaignRule().withUlogin(CLIENT).withMediaType(CampaignTypeEnum.TEXT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule, bannersRule2, campaignRule);

    private CopyCampRequestBean copyCampRequestBean;
    private Long campaignId;
    private CSRFToken csrfToken;

    @Before
    public void before() {
        cmdRule.getApiStepsRule().as(MANAGER);
        campaignId = bannersRule.getCampaignId();

        PropertyLoader<CopyCampRequestBean> propertyLoader = new PropertyLoader<>(CopyCampRequestBean.class);
        copyCampRequestBean = propertyLoader.getHttpBean("copyCampRequestDefaultBean");
        copyCampRequestBean.setCidFrom(String.valueOf(campaignId));
        copyCampRequestBean.setNewLogin(CLIENT);
        copyCampRequestBean.setOldLogin(CLIENT);
        cmdRule.oldSteps().onPassport().authoriseAs(MANAGER, User.get(MANAGER).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(MANAGER).getPassportUID());
    }

    @Test
    @Description("Проверяем удаление пробелов слева от cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10320")
    @Ignore("не работает")
    public void deleteLeftBackspaceTest() {
        copyCampRequestBean.setCidFrom("  " + String.valueOf(campaignId));
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkRedirect(response, CMD.COPY_CAMP);
    }

    @Test
    @Description("Проверяем удаление пробелов справа от cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10321")
    @Ignore("не работает")
    public void deleteRightBackspaceTest() {
        copyCampRequestBean.setCidFrom(String.valueOf(campaignId) + "  ");
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkRedirect(response, CMD.COPY_CAMP);
    }

    @Test(expected = AssertionError.class)
    @Description("Проверяем удаление пробелов в cid. баг https://st.yandex-team.ru/DIRECT-40548")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10309")
    public void deleteCenterBackspaceTest() {
        copyCampRequestBean.setCidFrom(String.valueOf(campaignId) + " " + String.valueOf(campaignId));
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        //TODO указать нужное сообщение об ошибке после исправления бага
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                TextResourceFormatter.resource(CopyCampErrors.NOT_ALL_FIELDS_FILLED).toString()));
    }

    @Test
    @Description("Проверяем валидацию при не заданном cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10308")
    public void emptyCidValidationTest() {
        copyCampRequestBean.setCidFrom(null);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                TextResourceFormatter.resource(CopyCampErrors.EMTY_CID).toString()));
    }

    @Test
    @Description("Проверяем валидацию при не заданном старом логине")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10310")
    public void emptyOldLoginValidationTest() {
        copyCampRequestBean.setOldLogin(null);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                TextResourceFormatter.resource(CopyCampErrors.NOT_ALL_FIELDS_FILLED).toString()));
    }

    @Test
    @Description("Проверяем валидацию при не заданном новом логине")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10311")
    public void emptyNewLoginValidationTest() {
        copyCampRequestBean.setNewLogin(null);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                TextResourceFormatter.resource(CopyCampErrors.NOT_ALL_FIELDS_FILLED).toString()));
    }

    @Test
    @Description("Проверяем валидацию при несуществующем у данного менеджера клиенте в качестве старого логина")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10312")
    public void notExistOldLoginValidationTest() {
        String login = "oldlogin";
        copyCampRequestBean.setOldLogin(login);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.NOT_EXIST_CLIENT).toString(), login)));
    }

    @Test
    @Description("Проверяем валидацию при несуществующем  у данного менеджера клиенте в качестве нового логина")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10313")
    public void notExistNewLoginValidationTest() {
        String login = "newLogin";
        copyCampRequestBean.setNewLogin(login);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.NOT_EXIST_CLIENT).toString(), login)));
    }

    @Test
    @Description("Проверяем валидацию при несуществующем  у данного менеджера номере кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10314")
    public void notExistCidValidationTest() {
        String cid = "11";
        copyCampRequestBean.setCidFrom(cid);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.NOT_EXIST_CID).toString(), cid)));
    }

    @Test
    @Description("Проверяем валидацию при номере кампании, не принадлежащей старому логину")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10315")
    public void otherClientCidValidationTest() {
        String cid = "263";
        copyCampRequestBean.setCidFrom(cid);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.OTHER_CLIENT_CID).toString(), cid, CLIENT)));
    }

    @Test
    @Description("Проверяем валидацию при копировании архивной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10316")
    public void archiveCampaignValidationTest() {
        copyCampRequestBean.setCopyArchived("0");
        copyCampRequestBean.setCopyModerateStatus("0");
        Long archiveCampaignId = bannersRule2.getCampaignId();
        cmdRule.getApiStepsRule().as(Logins.SUPER);
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignStopped(archiveCampaignId);
        cmdRule.apiAggregationSteps().campaignsArchive(CLIENT, archiveCampaignId);
        copyCampRequestBean.setCidFrom(String.valueOf(archiveCampaignId));
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.ARCHIVE_CAMPAIGN).toString(),
                        String.valueOf(archiveCampaignId))));
    }


    @Test
    @Description("Проверяем валидацию при копировании кампании без баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10319")
    public void campaignWithoutBannersValidationTest() {
        cmdRule.getApiStepsRule().as(Logins.SUPER);
        Long campaignWithoutBannersId = bannersRule.getCampaignId();
        cmdRule.apiSteps().adsSteps().adsDelete(CLIENT, bannersRule.getBannerId());
        copyCampRequestBean.setCidFrom(String.valueOf(campaignWithoutBannersId));
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.NOT_COPY_CAMPAIGNS).toString(),
                        String.valueOf(campaignWithoutBannersId))));
    }

    @Test
    @Description("Проверяем валидацию при новом клиенте, который не может иметь кампаний (менеджер)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10317")
    public void cantHaveCampaignValidationTest() {
        copyCampRequestBean.setNewLogin(MANAGER);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.CAN_NOT_HAVE_CAMPAIGN).toString(), MANAGER)));
    }

    @Test
    @Description("Проверяем валидацию при новом клиенте, который не является клиентом данного менеджера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10318")
    public void notClientValidationTest() {
        copyCampRequestBean.setNewLogin(NOT_CLIENT);
        DirectResponse response = cmdRule.oldSteps().onCopyCamp().copyCamp(csrfToken, copyCampRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                String.format(TextResourceFormatter.resource(CopyCampErrors.NOT_YOUR_CLIENT).toString(), NOT_CLIENT)));
    }
}
