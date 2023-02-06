package ru.yandex.autotests.direct.cmd.campaigns.statusbssynced;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.DateUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * TESTIRT-8297
 */
@Aqua.Test
@Description("Сброс statusBsSynced для кампании при создании/изменении кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(TrunkTag.YES)
public class CampaignStatusBsSynced {

    private static final String CLIENT = "at-direct-bssynced-camp-1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;
    private SaveCampRequest saveCampRequest;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
    }

    @Test
    @Description("statusBsSynced для новой кампании должен быть сброшен ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9562")
    public void statusBsSyncedForNewCamp() {
        checkStatusBsSynced(StatusBsSynced.NO.toString());
    }

    @Test
    @Description("statusBsSynced при изменении имени кампании должен быть сброшен ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9561")
    public void changeNameCamp() {
        makeCampSynced();
        saveCampRequest = getCamp();
        saveCampRequest.setName(saveCampRequest.getName() + "New");
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        checkStatusBsSynced(StatusBsSynced.NO.toString());
    }

    @Test
    @Description("statusBsSynced при изменении времени начала кампании должен быть сброшен ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9563")
    public void changeCampStartTimeStart() {
        makeCampSynced();
        saveCampRequest = getCamp();
        DateTime today = DateTime.now();
        saveCampRequest.setStart_date(today.toString(DateUtils.PATTERN_YYYY_MM_DD));
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        checkStatusBsSynced(StatusBsSynced.NO.toString());
    }

    private SaveCampRequest getCamp() {
        return bannersRule.getSaveCampRequest()
                .withCid(campaignId.toString())
                .withUlogin(CLIENT);
    }

    private void checkStatusBsSynced(String expStatus) {
        ShowCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
        assertThat("статус синхронизации кампании с БК соответствует ожидаемому", campResponse.getStatusBsSynced(),
                equalTo(expStatus));
    }

    private void makeCampSynced() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setBSSynced(campaignId.intValue(), true);
        cmdRule.apiSteps().groupFakeSteps().setGroupFakeStatusBsSynced(bannersRule.getGroupId(), Status.YES);
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannersRule.getBannerId(), Status.YES);
        CampaignsRecord campaign = TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(campaignId);
        assumeThat("кампания клиента синхронизирована с БК", campaign.getStatusbssynced(),
                equalTo(CampaignsStatusbssynced.Yes));
    }
}
