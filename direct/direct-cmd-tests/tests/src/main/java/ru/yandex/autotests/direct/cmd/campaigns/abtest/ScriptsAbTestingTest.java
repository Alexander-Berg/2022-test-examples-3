package ru.yandex.autotests.direct.cmd.campaigns.abtest;

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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//TESTIRT-9619
@Aqua.Test
@Description("запуск скрипта ppcManageExperiments.pl")
@Stories(TestFeatures.Campaigns.CREATE_AB_TEST)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.CREATE_AB_TEST)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class ScriptsAbTestingTest {
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
        percent = 70;
        abTestingHelper = new AbTestingHelper().withClient(CLIENT).withCmdRule(cmdRule);

        abTestingHelper.makeAllModerate(bannersRuleFirst.getCampaignId(),
                bannersRuleFirst.getGroupId(), bannersRuleFirst.getBannerId());

        abTestingHelper.makeAllModerate(bannersRuleSecond.getCampaignId(),
                bannersRuleSecond.getGroupId(), bannersRuleSecond.getBannerId());
    }

    @Test
    @Description("проверка статуса до начала")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9387")
    public void beforeStart() {
        experiment = abTestingHelper.saveWithDefaultDates(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        ExperimentsStatus actualStatus = TestEnvironment.newDbSteps().experimentsSteps()
                .getExperimentsRecord(experiment.getData().getExperimentId()).getStatus();
        assertThat("статус эксперимента соответствует ожиданиям", actualStatus, equalTo(ExperimentsStatus.New));
    }

    @Test
    @Description("запуск скрипта по неначатому эксперементу с завтрашней датой старта")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9386")
    public void startScriptBeforeDate() {
        experiment = abTestingHelper.saveWithDefaultDates(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        abTestingHelper.startAndCheck(equalTo(ExperimentsStatus.New), experiment);
    }


    @Test
    @Description("запуск скрипта по неначатому эксперементу с сегодняшней датой старта")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9388")
    public void startScriptInDate() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        experiment = abTestingHelper.saveWithAllDateFrom(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(), today);

        abTestingHelper.startAndCheck(equalTo(ExperimentsStatus.Started), experiment);
    }

    @Test
    @Description("запуск скрипта по неначатому эксперементу с вчерашней датой старта")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9389")
    public void startScriptAfterDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date today = cal.getTime();
        experiment = abTestingHelper.saveWithAllDateFrom(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(), today);

        abTestingHelper.startAndCheck(equalTo(ExperimentsStatus.Started), experiment);
    }


    @Test
    @Description("остановка скрипта по неначатому эксперементу с вчерашней датой старта")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9390")
    public void stopScriptAfterDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        Date dateFrom = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date dateTo = cal.getTime();
        experiment = abTestingHelper.saveWithAllDates(percent, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId(), dateFrom, dateTo);
        abTestingHelper.startAndCheck(equalTo(ExperimentsStatus.Stopped), experiment);
    }
}
