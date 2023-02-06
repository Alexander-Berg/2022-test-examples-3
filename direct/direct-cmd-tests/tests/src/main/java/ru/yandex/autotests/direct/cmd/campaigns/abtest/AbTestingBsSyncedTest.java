package ru.yandex.autotests.direct.cmd.campaigns.abtest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.CreateABTestResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.campaings.AbTestingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps.DATE_FORMAT;
import static ru.yandex.autotests.directapi.matchers.beans.EveryItem.everyItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Тесты создания эксперимента")
@Stories(TestFeatures.Campaigns.CREATE_AB_TEST)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.CREATE_AB_TEST)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class AbTestingBsSyncedTest {

    protected static final String CLIENT = "at-direct-backend-rus-os9";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static Integer SHARD;
    protected BannersRule bannersRuleFirst = new TextBannersRule().withUlogin(CLIENT);
    protected BannersRule bannersRuleSecond = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRuleFirst, bannersRuleSecond);
    private int percent;
    private CreateABTestResponse experiment;
    private AbTestingHelper abTestingHelper;

    @Before
    public void before() {
        SHARD = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);

        percent = 70;
        abTestingHelper = new AbTestingHelper().withClient(CLIENT).withCmdRule(cmdRule);

        abTestingHelper.makeAllModerate(bannersRuleFirst.getCampaignId(),
                bannersRuleFirst.getGroupId(), bannersRuleFirst.getBannerId());

        abTestingHelper.makeAllModerate(bannersRuleSecond.getCampaignId(),
                bannersRuleSecond.getGroupId(), bannersRuleSecond.getBannerId());
    }

    @Test
    @Description("проверка сбрасывания статуса синхронизации")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9376")
    public void checkBsSynced() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        experiment = abTestingHelper
                .saveWithAllDateFrom(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(),
                        today);
        tryResetBsSyncedStatusAndCheck(equalTo(CampaignsStatusbssynced.No));
    }

    @Test
    @Description("проверка несбрасывания статуса синхронизации до начала эксперимента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9377")
    public void checkBsSyncedBeforeDateOfStart() {
        experiment = abTestingHelper
                .saveWithDefaultDates(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        tryResetBsSyncedStatusAndCheck(equalTo(CampaignsStatusbssynced.Yes));

    }

    @Test
    @Description("проверка сбрасывания статуса синхронизации при завершении эксперимента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9378")
    public void checkBsSyncedAfterFinish() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        Date dateFrom = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date dateTo = cal.getTime();
        experiment = abTestingHelper
                .saveWithAllDateFrom(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(),
                        dateFrom);
        cmdRule.darkSideSteps().getRunScriptSteps()
                .runPpcManageExperiments(SHARD, experiment.getData().getExperimentId());

        TestEnvironment.newDbSteps().experimentsSteps()
                .setExperimentsDateTo(experiment.getData().getExperimentId(),
                        java.sql.Date.valueOf(new SimpleDateFormat(DATE_FORMAT).format(dateTo))
                );

        tryResetBsSyncedStatusAndCheck(equalTo(CampaignsStatusbssynced.No));

    }

    @Test
    @Description("проверка сбрасывания статуса синхронизации после завершения эксперимента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9379")
    public void checkBsSyncedAfterDateEnd() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        Date dateFrom = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date dateTo = cal.getTime();

        experiment = abTestingHelper
                .saveWithAllDates(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(),
                        dateFrom, dateTo);
        cmdRule.darkSideSteps().getRunScriptSteps()
                .runPpcManageExperiments(SHARD, experiment.getData().getExperimentId());
        tryResetBsSyncedStatusAndCheck(equalTo(CampaignsStatusbssynced.Yes));
    }

    @SuppressWarnings("unchecked")
    private void tryResetBsSyncedStatusAndCheck(Matcher<CampaignsStatusbssynced> matcher) {
        abTestingHelper.makeCampSynced(bannersRuleFirst.getCampaignId(),
                Collections.singletonList(bannersRuleFirst.getGroupId()),
                Collections.singletonList(bannersRuleFirst.getBannerId())
        );
        abTestingHelper.makeCampSynced(bannersRuleSecond.getCampaignId(),
                Collections.singletonList(bannersRuleSecond.getGroupId()),
                Collections.singletonList(bannersRuleSecond.getBannerId())
        );
        cmdRule.darkSideSteps().getRunScriptSteps()
                .runPpcManageExperiments(SHARD, experiment.getData().getExperimentId());

        CampaignsRecord campaignFisrt =
                TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(bannersRuleFirst.getCampaignId());
        CampaignsRecord campaignSecond =
                TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(bannersRuleSecond.getCampaignId());

        assertThat("статус синхронищации кампаний клиента с БК соответсвует ожиданиям",
                new CampaignsStatusbssynced[]{
                        campaignFisrt.getStatusbssynced(),
                        campaignSecond.getStatusbssynced()
                },
                everyItem(matcher));
    }
}
