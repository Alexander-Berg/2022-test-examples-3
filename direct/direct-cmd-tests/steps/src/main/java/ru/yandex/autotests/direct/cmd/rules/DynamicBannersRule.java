package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupSource;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.runDeleteCampaignScriptAndIgnoreResult;

public class DynamicBannersRule extends BannersRule {

    private Long feedId;
    private DynamicGroupSource source;

    public DynamicBannersRule() {
        super(CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2);
        withMediaType(CampaignTypeEnum.DTO);
        withSource(DynamicGroupSource.DOMAIN);

    }

    public Long getFeedId() {
        return feedId;
    }

    public DynamicBannersRule withSource(DynamicGroupSource source) {
        this.source = source;
        return this;
    }

    @Override
    public void createGroup() {
        Group group = getGroup();
        group.setCampaignID(campaignId.toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));

        switch (source) {
            case FEED:
                fillFeedGroup(group);
                break;
        }

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(ulogin, campaignId, group);
        saveGroup(groupRequest);
    }

    @Override
    public void saveGroup(GroupsParameters request) {
        getDirectCmdSteps().groupsSteps().postSaveDynamicAdGroups(request);
    }

    @Override
    public Group getCurrentGroup() {
        return getDirectCmdSteps().groupsSteps().getEditDynamicAdGroups(ulogin, campaignId, groupId)
                .getCampaign().getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"));
    }

    @Override
    protected void finish() {
        super.finish();

        switch (source) {
            case FEED:
                deleteFeed();
                break;
        }
    }

    private void fillFeedGroup(Group group) {
        createFeed();
        group.withMainDomain("").withHasFeedId("1").withFeedId(String.valueOf(feedId));
        group.getDynamicConditions().get(0).withConditions(BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_PERFORMANCE_DEFAULT2, Group.class)
                .getPerformanceFilters().get(0)
                .getConditions());
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
            runDeleteCampaignScriptAndIgnoreResult(DirectCmdRule.defaultClassRule(),
                    Long.parseLong(User.get(ulogin).getClientID()),
                    campaignId);

            TestEnvironment.newDbSteps().useShardForLogin(ulogin).feedsSteps()
                    .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(ulogin).getClientID(), feedId);
            getDirectCmdSteps().ajaxDeleteFeedsSteps().deleteFeed(ulogin, feedId);
        }
    }
}
