package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.JsonRedirectResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.mapEditGroupResponseToSaveRequest;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class PerformanceBannersRule extends BannersRule {

    private Long feedId;
    private Long creativeId;

    public PerformanceBannersRule() {
        super(CmdBeans.COMMON_REQUEST_GROUP_PERFORMANCE_DEFAULT2);
        withMediaType(CampaignTypeEnum.DMO);
    }

    public Long getFeedId() {
        return feedId;
    }

    public Long getCreativeId() {
        return creativeId;
    }

    @Override
    public void createGroup() {
        createFeed();
        createCreative();
        Group group = getGroupRequest();

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(ulogin, campaignId, group);
        saveGroup(groupRequest);
    }

    public Group getGroupRequest() {
        Group group = getGroup();
        group.setCampaignID(campaignId.toString());
        group.setFeedId(String.valueOf(feedId));
        group.getBanners().get(0)
                .getCreativeBanner().withCreativeId(creativeId);
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));

        return group;
    }

    @Override
    public void saveGroup(GroupsParameters request) {
        JsonRedirectResponse response = getDirectCmdSteps().groupsSteps().postSavePerformanceAdGroups(request);
        assumeThat("группа сохранилась", response.getResult(), notNullValue());
        assumeThat("группа сохранилась", response.getLocation(), notNullValue());
    }

    @Override
    public Group getCurrentGroup() {
        return mapEditGroupResponseToSaveRequest(getDirectCmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(ulogin, String.valueOf(campaignId),
                        String.valueOf(groupId), String.valueOf(bannerId))
                .getCampaign().getPerformanceGroups())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"));
    }

    @Override
    protected void finish() {
        try {
            TestEnvironment.newDbSteps().useShardForLogin(ulogin).campaignsSteps().deletePerformanceCampaign(campaignId);
            TestEnvironment.newDbSteps().campaignsSteps().deleteCampaign(campaignId);
        } catch (Throwable e) {
            log.info("Error while deleting performance campaign", e);
        }

        try {
            deleteFeed();
        } catch (Throwable e) {
            log.info("Error while deleting feeds", e);
        }
        try {
            deleteCreative();
        } catch (Throwable e) {
            log.info("Error while deleting creatives", e);
        }
    }

    private void createFeed() {
        FeedsRecord defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(ulogin).getClientID());
        defaultFeed.setUpdateStatus(FeedsUpdateStatus.Done);
        defaultFeed.setOffersCount(2L);

        feedId = TestEnvironment.newDbSteps().useShardForLogin(ulogin).feedsSteps()
                .createFeed(defaultFeed, User.get(ulogin).getClientID());
    }

    private void deleteFeed() {
        if (feedId != null) {
            TestEnvironment.newDbSteps().feedsSteps().deleteAdgroupsPerformanceRecords(feedId);
            TestEnvironment.newDbSteps().feedsSteps()
                    .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(ulogin).getClientID(), feedId);
            getDirectCmdSteps().ajaxDeleteFeedsSteps().deleteFeed(ulogin, feedId);
        }
    }

    private void createCreative() {
        creativeId = TestEnvironment.newDbSteps().useShardForLogin(ulogin).perfCreativesSteps()
                .saveDefaultPerfCreative(Long.parseLong(User.get(ulogin).getClientID()));
    }

    private void deleteCreative() {
        if (creativeId != null) {
            TestEnvironment.newDbSteps().bannersPerformanceSteps().deleteBannersPerformanceRecord(creativeId);
            TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
        }
    }
}
