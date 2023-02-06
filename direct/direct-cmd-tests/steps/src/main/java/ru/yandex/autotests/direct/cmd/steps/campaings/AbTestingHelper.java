package ru.yandex.autotests.direct.cmd.steps.campaings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.CreateABTestResponse;
import ru.yandex.autotests.direct.cmd.data.campaigns.Experiment;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ExperimentsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps.DATE_FORMAT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


public class AbTestingHelper {

    private String client;

    private Integer shard;

    public String getClient() {
        return client;
    }

    public AbTestingHelper withClient(String client) {
        this.client = client;
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(client);
        return this;
    }

    private DirectCmdRule cmdRule;

    public DirectCmdRule getCmdRule() {
        return cmdRule;
    }

    public AbTestingHelper withCmdRule(DirectCmdRule cmdRule) {
        this.cmdRule = cmdRule;
        return this;
    }


    public void check(Long experimentId, Integer percent, Long firstCampId, Long secondCampId,
            Experiment actualExperiment)
    {
        Experiment expectedExperiment = new Experiment()
                .withExperimentId(experimentId)
                .withPercent(percent)
                .withPrimaryCid(firstCampId)
                .withSecondaryCId(secondCampId);
        assertThat("данные эксперимента соотвествуют ожиданиям", actualExperiment,
                beanDiffer(expectedExperiment)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    public CreateABTestResponse saveWithAllDates(Integer percent, Long firstCampId,
            Long secondCampId, Date realDateFrom, Date readDateTo)
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);

        Date fakeStartDate = cal.getTime();
        cal.add(Calendar.DATE, 21);
        Date fakeEndDate = cal.getTime();
        CreateABTestResponse response = cmdRule.cmdSteps().campaignSteps().createABTest(
                client,
                firstCampId,
                secondCampId,
                percent,
                fakeStartDate,
                fakeEndDate
        );
        TestEnvironment.newDbSteps().useShardForLogin(client);
        TestEnvironment.newDbSteps().experimentsSteps()
                .setExperimentsDateFrom(response.getData().getExperimentId(),
                        java.sql.Date.valueOf(new SimpleDateFormat(DATE_FORMAT).format(realDateFrom))
                );
        TestEnvironment.newDbSteps().experimentsSteps()
                .setExperimentsDateTo(response.getData().getExperimentId(),
                        java.sql.Date.valueOf(new SimpleDateFormat(DATE_FORMAT).format(readDateTo))
                );
        return response;
    }

    public CreateABTestResponse saveWithAllDateFrom(Integer percent, Long firstCampId, Long secondCampId,
            Date realDateFrom)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(realDateFrom);
        cal.add(Calendar.DATE, 10);
        Date realDateTo = cal.getTime();
        return saveWithAllDates(percent, firstCampId, secondCampId, realDateFrom, realDateTo);
    }

    public CreateABTestResponse saveWithDefaultDates(Integer percent, Long firstCampId, Long secondCampId) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        Date fromDate = cal.getTime();
        cal.add(Calendar.DATE, 10);
        Date toDate = cal.getTime();
        return cmdRule.cmdSteps().campaignSteps().createABTest(
                client,
                firstCampId,
                secondCampId,
                percent,
                fromDate,
                toDate
        );
    }

    public void saveAndCheck(Integer percent, Long firstCampId, Long secondCampId) {
        CreateABTestResponse createABTestResponse = saveWithDefaultDates(percent, firstCampId, secondCampId);
        Experiment actualExperiment = cmdRule.cmdSteps().campaignSteps().showExperiment(client)
                .getExperimentMap().get(createABTestResponse.getData().getExperimentId());

        check(createABTestResponse.getData().getExperimentId(), percent, firstCampId, secondCampId,
                actualExperiment);
    }

    public void startAndCheck(Matcher<ExperimentsStatus> matcher, CreateABTestResponse experiment) {
        cmdRule.darkSideSteps().getRunScriptSteps()
                .runPpcManageExperiments(shard, experiment.getData().getExperimentId());
        ExperimentsStatus actualStatus = TestEnvironment.newDbSteps().experimentsSteps()
                .getExperimentsRecord(experiment.getData().getExperimentId()).getStatus();
        assertThat("статус эксперимента соответствует ожиданиям", actualStatus, matcher);
    }

    public void makeAllModerate(Long campaignId, Long groupId, Long bannerId) {
        cmdRule.darkSideSteps().getCampaignFakeSteps().makeCampaignFullyModerated(campaignId);
        cmdRule.darkSideSteps().getGroupsFakeSteps().makeGroupFullyModerated(groupId);
        cmdRule.darkSideSteps().getBannersFakeSteps().makeBannerFullyModerated(bannerId);
    }

    public void createGroup(Long campaignId) {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2, Group.class);
        group.setCampaignID(campaignId.toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));


        GroupsParameters groupRequest = GroupsParameters.forNewCamp(
                client, campaignId, group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupRequest);
    }

    public Long getGroupId(Long campaignId) {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(client,
                campaignId.toString());
        return showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"))
                .getAdGroupId();
    }

    public Long getBannerId(Long campaignId) {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(client,
                campaignId.toString());
        return showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть баннер"))
                .getBid();
    }


    public void makeCampSynced(Long campaignId, List<Long> groupIds, List<Long> bannerIds) {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setBSSynced(campaignId.intValue(), true);
        groupIds.stream().forEach(groupId ->
                cmdRule.apiSteps().groupFakeSteps().setGroupFakeStatusBsSynced(groupId, Status.YES));
        bannerIds.stream().forEach(bannerId ->
                cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES));
        CampaignsRecord campaign = TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(campaignId);
        assumeThat("кампания клиента синхронизирована с БК", campaign.getStatusbssynced(),
                equalTo(CampaignsStatusbssynced.Yes));
    }
}
