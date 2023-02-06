package ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class EditAdGroupsMobileContentRequest extends BasicDirectRequest {

    public static EditAdGroupsMobileContentRequest forSingleBanner(String uLogin, long campaignId,
                                                                   long groupId, long bannerId) {
        return new EditAdGroupsMobileContentRequest().
                withCid(campaignId).
                withAdGroupIds(String.valueOf(groupId)).
                withBid(String.valueOf(bannerId)).
                withBannerStatus("all").
                withUlogin(uLogin);
    }

    @SerializeKey("cid")
    private Long cid;
    @SerializeKey("bid")
    private String bid;
    @SerializeKey("banner_status")
    private String bannerStatus;
    @SerializeKey("adgroup_ids")
    private String adGroupIds;

    public Long getCid() {
        return cid;
    }

    public EditAdGroupsMobileContentRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getBid() {
        return bid;
    }

    public EditAdGroupsMobileContentRequest withBid(String bid) {
        this.bid = bid;
        return this;
    }

    public String getBannerStatus() {
        return bannerStatus;
    }

    public EditAdGroupsMobileContentRequest withBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
        return this;
    }

    public String getAdGroupIds() {
        return adGroupIds;
    }

    public EditAdGroupsMobileContentRequest withAdGroupIds(String adGroupIds) {
        this.adGroupIds = adGroupIds;
        return this;
    }
}
