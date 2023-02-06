package ru.yandex.autotests.direct.cmd.campaigns.abtest;

import java.util.Calendar;
import java.util.Date;

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
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ExperimentsStatus;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.runDeleteCampaignScriptAndCheckResult;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// таск:
@Aqua.Test
@Description("Удаление эксперимента из существующей кампании")
@Stories(TestFeatures.Campaigns.CREATE_AB_TEST)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.CREATE_AB_TEST)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class DeleteCampaignWithExperimentTest {
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
        percent = 60;
        abTestingHelper = new AbTestingHelper().withClient(CLIENT).withCmdRule(cmdRule);

        abTestingHelper.makeAllModerate(bannersRuleFirst.getCampaignId(),
                bannersRuleFirst.getGroupId(), bannersRuleFirst.getBannerId());

        abTestingHelper.makeAllModerate(bannersRuleSecond.getCampaignId(),
                bannersRuleSecond.getGroupId(), bannersRuleSecond.getBannerId());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);

        Date dateFrom = cal.getTime();
        cal.add(Calendar.DATE, 10);
        Date dateTo = cal.getTime();
        experiment = abTestingHelper.saveWithAllDates(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(),
                dateFrom, dateTo);

        cmdRule.darkSideSteps().getRunScriptSteps()
                .runPpcManageExperiments(SHARD, experiment.getData().getExperimentId());
    }

    @Test
    @Description("проверка удаления кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9385")
    public void deleteCamp() {
        cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, bannersRuleFirst.getCampaignId());
        runDeleteCampaignScriptAndCheckResult(cmdRule, Long.parseLong(User.get(CLIENT).getClientID()),
                bannersRuleFirst.getCampaignId());

        ExperimentsStatus actualStatus = TestEnvironment.newDbSteps().experimentsSteps()
                .getExperimentsRecord(experiment.getData().getExperimentId()).getStatus();
        assertThat("статус эксперимента соответствует ожиданиям", actualStatus, equalTo(ExperimentsStatus.Stopped));
    }
}
