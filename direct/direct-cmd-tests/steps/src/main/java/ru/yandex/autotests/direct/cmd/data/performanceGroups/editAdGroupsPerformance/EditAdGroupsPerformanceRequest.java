package ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class EditAdGroupsPerformanceRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private String cid;
    @SerializeKey("banner_status")
    private String bannerStatus;
    @SerializeKey("adgroup_ids")
    private String adGroupIds;
    @SerializeKey("bid")
    private String bids;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getBannerStatus() {
        return bannerStatus;
    }

    public void setBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
    }

    public String getAdGroupIds() {
        return adGroupIds;
    }

    public void setAdGroupIds(String adGroupIds) {
        this.adGroupIds = adGroupIds;
    }

    public String getBids() {
        return bids;
    }

    public void setBids(String bids) {
        this.bids = bids;
    }

    public EditAdGroupsPerformanceRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public EditAdGroupsPerformanceRequest withBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
        return this;
    }

    public EditAdGroupsPerformanceRequest withAdGroupIds(String adGroupIds) {
        this.adGroupIds = adGroupIds;
        return this;
    }

    public EditAdGroupsPerformanceRequest withBids(String bids) {
        this.bids = bids;
        return this;
    }
}
