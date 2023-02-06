package ru.yandex.autotests.direct.httpclient.campaigns.campUnarc;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.CampUnarcRequestBean;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.04.15
 *         https://st.yandex-team.ru/TESTIRT-4993
 */

@Aqua.Test
@Description("Проверка разархивирования кампании контроллером campUnarc")
@Stories(TestFeatures.Campaigns.CAMP_UNARC)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.CAMP_UNARC)
@Tag(OldTag.YES)
public class CampUnarcTest {

    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    TextBannersRule bannersRule1 = new TextBannersRule().withUlogin(CLIENT);
    TextBannersRule bannersRule2 = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2);


    private CampUnarcRequestBean campUnarcRequestBean;
    private Long firstCampaignId;
    private Long secondCampaignId;
    private CSRFToken csrfToken;

    @Before
    public void before() {
        firstCampaignId = bannersRule1.getCampaignId();
        secondCampaignId = bannersRule2.getCampaignId();

        cmdRule.apiSteps().campaignStepsV5().campaignsSuspend(CLIENT, firstCampaignId, secondCampaignId);
        cmdRule.apiSteps().campaignStepsV5().campaignsArchive(CLIENT, firstCampaignId, secondCampaignId);
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        campUnarcRequestBean = new CampUnarcRequestBean();
        campUnarcRequestBean.setTab("arch");
    }


    @Test
    @Description("Проверяем разархивирование кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10300")
    public void campaignUnarchiveTest() {
        campUnarcRequestBean.setCid(String.valueOf(firstCampaignId));
        cmdRule.oldSteps().onCampUnarc().unarchiveCampaign(csrfToken, campUnarcRequestBean);
        Campaign campaign = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, firstCampaignId);
        assertThat("Кампания разархивирована", campaign.getArchived(), equalTo("No"));
    }

    @Test
    @Description("Проверяем разархивирование двух кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10301")
    public void twoCampaignsUnarchiveTest() {
        campUnarcRequestBean.setCid(firstCampaignId.toString() + "," + secondCampaignId.toString());
        cmdRule.oldSteps().onCampUnarc().unarchiveCampaign(csrfToken, campUnarcRequestBean);
        Campaign campaign1 = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, firstCampaignId);
        Campaign campaign2 = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, secondCampaignId);

        assertThat("Первая кампания разархивирована", campaign1.getArchived(), equalTo("No"));
        assertThat("Вторая  кампания разархивирована", campaign2.getArchived(), equalTo("No"));
    }

    @Test
    @Description("Проверяем разархивирование неархивной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10302")
    public void noArchiveCampaignUnarchiveTest() {
        cmdRule.apiSteps().campaignStepsV5().campaignsUnarchive(CLIENT, firstCampaignId);
        campUnarcRequestBean.setCid(firstCampaignId.toString());
        DirectResponse response = cmdRule.oldSteps().onCampUnarc().unarchiveCampaign(csrfToken, campUnarcRequestBean);
        cmdRule.oldSteps().commonSteps().checkRedirect(response, CMD.SHOW_CAMPS);
        Campaign campaign = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, firstCampaignId);
        assertThat("Кампания разархивирована", campaign.getArchived(), equalTo("No"));
    }


}
