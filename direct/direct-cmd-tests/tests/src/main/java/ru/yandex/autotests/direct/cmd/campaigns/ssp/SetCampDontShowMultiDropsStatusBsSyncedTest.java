package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

// TESTIRT-8758
@Aqua.Test
@Description("Сброс статуса statusBsSynced при отключении/включении " +
        "показа на площадках и ssp-платформах на странице статистики")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SET_CAMP_DONT_SHOW_MULTI)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SetCampDontShowMultiDropsStatusBsSyncedTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    private static final String INIT_DONT_SHOW = "Smaato";
    private static final String[] TO_ENABLE = new String[]{"Smaato"};
    private static final String[] ENABLING_RESULT = new String[0];
    private static final String[] TO_DISABLE = new String[]{"Rubicon"};
    private static final String[] DISABLING_RESULT = new String[]{"Rubicon", "Smaato"};

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().
            overrideCampTemplate(new SaveCampRequest().withDontShow(INIT_DONT_SHOW)).
            withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.
            defaultRule().
            as(CLIENT).
            withRules(bannersRule);


    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);

        CampaignsRecord campaign =
                TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        campaign.setStatusbssynced(CampaignsStatusbssynced.Yes);
        TestEnvironment.newDbSteps().campaignsSteps().updateCampaigns(campaign);

        campaign = TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        assumeThat("перед тестом у кампании выставлен флаг statusBsSynced = Yes",
                campaign.getStatusbssynced(), equalTo(CampaignsStatusbssynced.Yes));
    }

    @Test
    @Description("Сброс статуса statusBsSynced при включении " +
            "показа на площадках и ssp-платформах на странице статистики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9557")
    public void testEnablingAtSetCampDontShowMultiDropsStatusBsSynced() {
        cmdRule.cmdSteps().setCampDontShowMultiSteps().enableShow(
                CLIENT, bannersRule.getCampaignId(), Arrays.asList(TO_ENABLE));
        check(ENABLING_RESULT);
    }

    @Test
    @Description("Сброс статуса statusBsSynced при отключении " +
            "показа на площадках и ssp-платформах на странице статистики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9558")
    public void testDisablingAtSetCampDontShowMultiDropsStatusBsSynced() {
        cmdRule.cmdSteps().setCampDontShowMultiSteps().disableShow(
                CLIENT, bannersRule.getCampaignId(), Arrays.asList(TO_DISABLE));
        check(DISABLING_RESULT);
    }

    private void check(String[] expectedDontShow) {
        EditCampResponse campResponse =
                cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        List<String> actualDontShow = Arrays.asList(campResponse.getCampaign().getDontShow());
        assumeThat("запрещенные площадки/ssp-платформы на уровне кампании изменились",
                actualDontShow, containsInAnyOrder(expectedDontShow));

        CampaignsRecord campaign = TestEnvironment.newDbSteps()
                .campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        assertThat("после вызова ручки setCampDontShowMulti у кампании сбросился флаг statusBsSynced",
                campaign.getStatusbssynced(), equalTo(CampaignsStatusbssynced.No));
    }
}
