package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkArgument;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PerformanceAdGroupInfo extends AdGroupInfo {

    private FeedInfo feedInfo;

    public PerformanceAdGroup getPerformanceAdGroup() {
        return (PerformanceAdGroup) getAdGroup();
    }

    @Override
    public PerformanceAdGroupInfo withAdGroup(AdGroup adGroup) {
        checkArgument(adGroup instanceof PerformanceAdGroup, "The argument must be an instance of PerformanceAdGroup");
        super.withAdGroup(adGroup);
        return this;
    }

    public FeedInfo getFeedInfo() {
        return feedInfo;
    }

    public PerformanceAdGroupInfo withFeedInfo(FeedInfo feedInfo) {
        this.feedInfo = feedInfo;
        return this;
    }

    @Override
    public PerformanceAdGroupInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public PerformanceAdGroupInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    public Long getAdGroupId() {
        return ifNotNull(getPerformanceAdGroup(), AdGroup::getId);
    }

    public Long getFeedId() {
        return getFeedInfo().getFeedId();
    }

    public Long getCampaignId() {
        return getCampaignInfo().getCampaignId();
    }

    public Long getOrderId() {
        return getCampaignInfo().getOrderId();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }
}
