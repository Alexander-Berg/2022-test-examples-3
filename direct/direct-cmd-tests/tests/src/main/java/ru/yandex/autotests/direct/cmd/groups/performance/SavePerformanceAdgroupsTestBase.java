package ru.yandex.autotests.direct.cmd.groups.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.JsonRedirectResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.performancefilters.PerformanceFiltersHelper;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.mapEditGroupResponseToSaveRequest;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.runDeleteCampaignScriptAndIgnoreResult;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public abstract class SavePerformanceAdgroupsTestBase {
    public static final String CLIENT = "at-direct-perf-groups";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    protected Long campaignId;
    protected SaveCampRequest saveCampRequest;
    protected Group expectedGroup;
    protected List<Long> creativeIds;
    protected Long feedId;
    protected String adgroupId;
    protected String bids;
    protected FeedsRecord defaultFeed;

    @Before
    public void before() {
        saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_NEW_PERFORMANCE_CAMP_DEFAULT, SaveCampRequest.class);
        saveCampRequest.setUlogin(CLIENT);
        campaignId = Long.valueOf(cmdRule.cmdSteps().campaignSteps().postSaveNewCamp(saveCampRequest)
                .getLocationParam(LocationParam.CID));

        defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        defaultFeed.setUpdateStatus(FeedsUpdateStatus.Done);
        defaultFeed.setOffersCount(2L);
        feedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());

        creativeIds = new ArrayList<>();
        creativeIds = createCreative(1);

        expectedGroup = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_PERFORMANCE_DEFAULT2, Group.class);
        expectedGroup.setFeedId(String.valueOf(feedId));
        expectedGroup.getBanners().get(0).getCreativeBanner().withCreativeId(creativeIds.get(0));
    }

    @After
    public void after() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().makeCampaignReadyForDelete(campaignId);
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, campaignId);
            runDeleteCampaignScriptAndIgnoreResult(cmdRule,
                    Long.parseLong(User.get(CLIENT).getClientID()),
                    campaignId);
        }
        try {
            deleteCreatives();
        } catch (Exception e) {
            cmdRule.log().info("Не удалось удалить креативы", e);
        }
        try {
            deleteFeed();
        } catch (Exception e) {
            cmdRule.log().info("Не удалось удалить фид", e);
        }
    }

    protected void deleteCreatives() {
        if (creativeIds.size() > 0) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().deletePerfCreatives(creativeIds);
        }
    }

    protected void deleteFeed() {
        if (feedId != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                    .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(CLIENT).getClientID(), feedId);
            cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(CLIENT, feedId);
        }
    }

    protected JsonRedirectResponse saveGroup() {
        GroupsParameters requestParams = GroupsParameters.forNewCamp(CLIENT, campaignId, expectedGroup);
        return cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(requestParams);
    }

    protected GroupErrorsResponse saveInvalidGroup() {
        GroupsParameters requestParams = GroupsParameters.forNewCamp(CLIENT, campaignId, expectedGroup);
        return cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroupsErrorResponse(requestParams);
    }

    protected List<Long> createCreative(int count) {
        List<Long> creativeIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            creativeIds.add(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .saveDefaultPerfCreative(User.get(CLIENT).getClientID()));
        }
        return creativeIds;
    }

    protected String getFirstAdgroupId() {
        return String.valueOf(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .getBannersByCid(campaignId).get(0).getPid());
    }

    protected String getBid() {
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .getBannersByCid(campaignId).stream().filter(t -> t.getPid().equals(Long.valueOf(adgroupId)))
                .findFirst().get().getBid().toString();
    }

    protected void check() {
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(CLIENT, String.valueOf(campaignId), adgroupId, bids);

        Group actualGroup = mapEditGroupResponseToSaveRequest(actualResponse.getCampaign().getPerformanceGroups()).get(0);
        if (expectedGroup.getMinusWords().isEmpty()) actualGroup.setMinusWords(Collections.emptyList());
        setNullExpectedGroupFields();
        PerformanceFiltersHelper.sortConditions(actualGroup.getPerformanceFilters());
        PerformanceFiltersHelper.sortConditions(expectedGroup.getPerformanceFilters());

        assertThat("группа в ответе контроллера соответствует сохраненной", actualGroup,
                beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    private void setNullExpectedGroupFields() {
        expectedGroup
                .withAdGroupID(null)
                .withTags(null)
                .withAutobudget(null)
                .withHrefParams(null)
                .withUsedCreativeIds(null);
        expectedGroup.getBanners().forEach(t -> {
            t.withBid(null);
            t.setStatusModerate(null);
        });
        expectedGroup.getPerformanceFilters().forEach(t -> t.withPerfFilterId(null));
    }
}
