package ru.yandex.autotests.direct.httpclient.campaigns.stopresumecamp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusshow;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.AjaxStopResumeCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.CampaignStopResumeActionEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.data.campaigns.CampaignStopResumeActionEnum.RESUME;
import static ru.yandex.autotests.direct.httpclient.data.campaigns.CampaignStopResumeActionEnum.STOP;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 12.11.14.
 * TESTIRT-3298
 */
@Aqua.Test
@Description("Тесты контроллера ajaxStopResumeCamp")
@RunWith(Parameterized.class)
@Stories(TestFeatures.Campaigns.AJAX_STOP_RESUME_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CmdTag.AJAX_STOP_RESUME_CAMP)
@Tag(OldTag.YES)
public class AjaxStopResumeCampTest {

    private final String DRAFT_CAMPAIGN_STATUS = "Черновик";
    private static final String CLIENT = "at-backend-stoprescamp";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withBanners(
                    Arrays.asList(BannersFactory.getDefaultTextBanner(), BannersFactory.getDefaultTextBanner())))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameter
    public CampaignStopResumeActionEnum action;

    private Long campaignId;
    private CSRFToken csrfToken;
    private AjaxStopResumeCampParameters params;
    private CampaignsStatusshow expectedStatusShow;

    @Parameterized.Parameters(name = "Действие с кампанией: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {STOP},
                {RESUME}
        });
    }

    @Before
    public void before() {
        params = new AjaxStopResumeCampParameters();
        params.setUlogin(CLIENT);
        campaignId = bannersRule.getCampaignId();

        switch (action) {
            case STOP:
                params.setDoStop("1");
                expectedStatusShow = CampaignsStatusshow.No;
                break;
            case RESUME:
                cmdRule.cmdSteps().campaignSteps().getStopCamp(CLIENT, campaignId);
                params.setDoStop("0");
                expectedStatusShow = CampaignsStatusshow.Yes;
                break;
        }
        params.setCid(campaignId.toString());

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
    }

    @Test
    @Description("Проверка ответа сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10423")
    public void stopResumeCampaignTest() {
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckStatus(csrfToken, params, equalTo(DRAFT_CAMPAIGN_STATUS));
    }

    @Test
    @Description("Проверка через бд")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10424")
    public void stopResumeCampaignDbCheckTest() {
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckStatus(csrfToken, params, equalTo(DRAFT_CAMPAIGN_STATUS));
        CampaignsRecord camp =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps().getCampaignById(campaignId);
        assertThat("статус кампании в апи соответствует ожидаемому", camp.getStatusshow(), equalTo(expectedStatusShow));
    }

    @Test
    @Description("Проверка через бд после выполнения одного действия дважды")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10425")
    public void stopResumeCampaignDoubleActionDbCheckTest() {
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckStatus(csrfToken, params, equalTo(DRAFT_CAMPAIGN_STATUS));
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckStatus(csrfToken, params, equalTo(DRAFT_CAMPAIGN_STATUS));

        CampaignsRecord camp =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps().getCampaignById(campaignId);
        assertThat("статус кампании в апи соответствует ожидаемому", camp.getStatusshow(), equalTo(expectedStatusShow));
    }
}
