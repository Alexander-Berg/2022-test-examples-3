package ru.yandex.autotests.direct.cmd.groups.performance;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps.extractCidFromSaveCampResponse;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка получения фидов при создании/редактировании группы")
@Stories(TestFeatures.Groups.FEEDS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.ADD_AD_GROUPS_PERFORMANCE)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class AddAdgroupPerformanceFeedsTest {
    public static final String CLIENT = "at-direct-perf-groups2";
    private static final Long OFFER_COUNT = 2L;

    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected SaveCampRequest saveCampRequest;
    private Group group;
    private Long campaignId;
    private Long newFeedId;
    private Long errorFeedId;
    private Long doneFeedId;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        FeedHelper.deleteAllFeeds(cmdRule, CLIENT);
        createFeeds();

        saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_NEW_PERFORMANCE_CAMP_DEFAULT, SaveCampRequest.class);
        saveCampRequest.setUlogin(CLIENT);
        group = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_PERFORMANCE_DEFAULT2, Group.class);

        Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultPerfCreative(User.get(CLIENT).getClientID());

        group.setFeedId(String.valueOf(doneFeedId));
        group.getBanners().get(0)
                .getCreativeBanner().withCreativeId(creativeId);

        campaignId = extractCidFromSaveCampResponse(cmdRule.cmdSteps().campaignSteps()
                .postSaveNewCamp(saveCampRequest));
        saveGroup();
    }

    @After
    public void after() {
        if (campaignId != null) {
            long adgroupId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                    .getBannersByCid(campaignId).get(0).getPid();
            cmdRule.apiAggregationSteps().makeCampaignReadyForDelete(campaignId);
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, campaignId);

            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adgroupsPerformanceSteps()
                    .deleteAdgroupsPerformance(adgroupId);
        }

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(CLIENT).getClientID(), newFeedId, errorFeedId,
                        doneFeedId);
        cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeeds(CLIENT, Arrays.asList(newFeedId, errorFeedId,
                doneFeedId));
    }

    @Test
    @Description("Проверка списка фидов при создании группы контроллером addPerformanceAdgroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9815")
    public void addPerformanceAdgroupFeedsTest() {
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getAddAdGroupsPerformance(CLIENT, String.valueOf(campaignId));
        for (Feed f : actualResponse.getFeeds()) {
            assertThat("Статус фида DONE", f.getUpdateStatus(),
                    containsString(FeedsUpdateStatus.Done.getLiteral()));
        }
    }


    private void createFeeds() {
        FeedsRecord feedsRecord = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        feedsRecord.setUpdateStatus(FeedsUpdateStatus.Error);
        feedsRecord.setOffersCount(0L);
        errorFeedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(feedsRecord, User.get(CLIENT).getClientID());

        feedsRecord.setOffersCount(OFFER_COUNT);
        feedsRecord.setUpdateStatus(FeedsUpdateStatus.New);
        newFeedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(feedsRecord, User.get(CLIENT).getClientID());

        feedsRecord.setUpdateStatus(FeedsUpdateStatus.Done);
        doneFeedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(feedsRecord, User.get(CLIENT).getClientID());
    }

    protected void saveGroup() {
        GroupsParameters requestParams = GroupsParameters.forNewCamp(CLIENT, campaignId, group);
        cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(requestParams);
    }
}
