package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

// TESTIRT-8758
@Aqua.Test
@Description("Сброс флага statusBsSynced кампании при редактировании запрещенных площадок и ssp-платформ")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
public class DontShowParamDropsStatusBsSyncedAtSaveCampTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String DONT_SHOW = "Smaato";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.
            defaultRule().
            as(CLIENT).
            withRules(bannersRule);


    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);

        // нужно, чтобы убедиться, что следующий запрос к saveCamp будет отличаться только 'DontShow'
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(bannersRule.
                getSaveCampRequest().
                withCid(bannersRule.getCampaignId().toString()));

        CampaignsRecord campaignsRecord = TestEnvironment.newDbSteps()
                .campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        campaignsRecord.setStatusbssynced(CampaignsStatusbssynced.Yes);
        TestEnvironment.newDbSteps()
                .campaignsSteps().updateCampaigns(campaignsRecord);

        campaignsRecord = TestEnvironment.newDbSteps()
                .campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        assumeThat("перед тестом у кампании выставлен флаг statusBsSynced = Yes",
                campaignsRecord.getStatusbssynced(), equalTo(CampaignsStatusbssynced.Yes));
    }

    @Test
    @Description("Сброс флага statusBsSynced кампании при редактировании запрещенных площадок и ssp-платформ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9554")
    public void testDontShowParamDropStatusBsSyncedAtSaveCamp() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(bannersRule.
                getSaveCampRequest().
                withDontShow(DONT_SHOW));

        CampaignsRecord campaign =
                TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        assertThat("после изменения параметра DontShow у кампании сбросился флаг statusBsSynced",
                campaign.getStatusbssynced(), equalTo(CampaignsStatusbssynced.No));
    }
}
