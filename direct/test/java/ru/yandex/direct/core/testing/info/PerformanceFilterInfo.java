package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.dbutil.model.ClientId;

public class PerformanceFilterInfo {

    private PerformanceFilter filter;
    private PerformanceAdGroupInfo adGroupInfo;

    public PerformanceFilter getFilter() {
        return filter;
    }

    public void setFilter(PerformanceFilter filter) {
        this.filter = filter;
    }

    public PerformanceFilterInfo withFilter(PerformanceFilter filter) {
        setFilter(filter);
        return this;
    }

    public Long getFilterId() {
        return filter.getId();
    }

    public PerformanceFilterInfo withAdGroupInfo(PerformanceAdGroupInfo adGroupInfo) {
        setAdGroupInfo(adGroupInfo);
        return this;
    }

    public PerformanceAdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public void setAdGroupInfo(PerformanceAdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
    }

    public Long getAdGroupId() {
        return adGroupInfo.getAdGroupId();
    }


    public FeedInfo getFeedInfo() {
        return adGroupInfo.getFeedInfo();
    }

    public Long getFeedId() {
        return adGroupInfo.getFeedId();
    }

    public CampaignInfo getCampaignInfo() {
        return adGroupInfo.getCampaignInfo();
    }

    public Long getCampaignId() {
        return adGroupInfo.getCampaignId();
    }

    public ClientInfo getClientInfo() {
        return adGroupInfo.getClientInfo();
    }

    public ClientId getClientId() {
        return adGroupInfo.getClientId();
    }

    public Integer getShard() {
        return adGroupInfo.getShard();
    }
}
