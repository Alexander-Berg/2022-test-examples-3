package ru.yandex.autotests.direct.httpclient.campaigns.campUnarc;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientLimitsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.CampUnarcRequestBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignUnarcErrors;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.04.15
 *         https://st.yandex-team.ru/TESTIRT-4993
 */

@Aqua.Test
@Description("Проверка валидации при разархивировании кампании контроллером campUnarc")
@Stories(TestFeatures.Campaigns.CAMP_UNARC)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.CAMP_UNARC)
@Tag(OldTag.YES)
public class CampaignUnacrcValidationTest {

    public static final String CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT = "at-direct-b-campcountexceed";
    private static final Integer CLIENT_ID_FOR_MAX_COUNT_CAMPAIGNS_CLIENT = 7234708;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    TextBannersRule bannersRule1 = new TextBannersRule().withUlogin(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT);
    TextBannersRule bannersRule2 = new TextBannersRule().withUlogin(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(
            new TestWatcher() {
                @Override
                protected void starting(org.junit.runner.Description description) {
                    clearCamps();
                    resetClientLimits();
                }
            }, bannersRule1, bannersRule2
    );
    private CampUnarcRequestBean campUnarcRequestBean;
    private CSRFToken csrfToken;

    private void clearCamps() {
        cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT);
    }

    private void resetClientLimits() {
        setClientLimits(Long.valueOf(CLIENT_ID_FOR_MAX_COUNT_CAMPAIGNS_CLIENT), 0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Before
    public void before() {
        cmdRule.apiSteps().campaignStepsV5()
                .campaignsSuspend(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT, bannersRule1.getCampaignId());
        cmdRule.apiSteps().campaignStepsV5()
                .campaignsArchive(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT, bannersRule1.getCampaignId());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT,
                User.get(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT_WITH_MAX_NOARCHIVE_CAMPAIGN_COUNT).getPassportUID());
        campUnarcRequestBean = new CampUnarcRequestBean();
        campUnarcRequestBean.setTab("all");
    }

    @Test
    @Description("Проверяем валидацию при разархивировании кампании c пустым номером кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10303")
    public void campaignUnarchiveEmptyCidValidationTest() {
        campUnarcRequestBean.setCid(null);
        DirectResponse response = cmdRule.oldSteps().onCampUnarc().unarchiveCampaign(csrfToken, campUnarcRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Проверяем валидацию при разархивировании кампании c неправильным номером кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10304")
    public void campaignUnarchiveIncorrectCidValidationTest() {
        String incorrectCid = "abc";
        campUnarcRequestBean.setCid(incorrectCid);
        DirectResponse response = cmdRule.oldSteps().onCampUnarc().unarchiveCampaign(csrfToken, campUnarcRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, String.format(
                TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_CID).toString(), incorrectCid));
    }

    private void setClientLimits(Long clientId,
            Long bannerCountLimit,
            Long campCountLimit,
            Long unarcCampCountLimit,
            Long feedCountLimit,
            Long feedMaxFileSize,
            Long keywordCountLimit)
    {
        ClientLimitsRecord clientLimits = new ClientLimitsRecord();
        clientLimits.setClientid(clientId);
        clientLimits.setBannerCountLimit(bannerCountLimit);
        clientLimits.setCampCountLimit(campCountLimit);
        clientLimits.setUnarcCampCountLimit(unarcCampCountLimit);
        clientLimits.setFeedCountLimit(feedCountLimit);
        clientLimits.setFeedMaxFileSize(feedMaxFileSize);
        clientLimits.setKeywordCountLimit(keywordCountLimit);
        Integer clientShard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);
        TestEnvironment.newDbSteps().useShard(clientShard).clientsLimitsSteps().addOrUpdateClientLimits(clientLimits);
    }

    @Test
    @Description(
            "Проверяем валидацию при разархивирование кампании для клиента, у которого достигнут максимальный порог по "
                    +
                    "числу активных кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10305")
    public void maxNoArchiveClientLimitsCampaignLimitExceedValidationTest() {
        setClientLimits(Long.valueOf(CLIENT_ID_FOR_MAX_COUNT_CAMPAIGNS_CLIENT), 0L, 0L, 1L, 0L, 0L, 0L);
        campUnarcRequestBean.setCid(String.valueOf(bannersRule1.getCampaignId()));
        DirectResponse response = cmdRule.oldSteps().onCampUnarc().unarchiveCampaign(csrfToken, campUnarcRequestBean);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(
                TextResourceFormatter.resource(CampaignUnarcErrors.MAX_ACTIVE_CAMPAIGN_COUNT_EXCEED).toString()));
    }
}
