package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ArchiveBannerRequest extends BasicDirectRequest {
    @SerializeKey("adgroup_ids")
    private String adGroupIds;

    @SerializeKey("bid")
    private String bid;

    @SerializeKey("cid")
    private String cid;

    public String getAdGroupIds() {
        return adGroupIds;
    }

    public void setAdGroupIds(String adGroupIds) {
        this.adGroupIds = adGroupIds;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public ArchiveBannerRequest withBid(String bid) {
        this.bid = bid;
        return this;
    }

    public ArchiveBannerRequest withAdGroupIds(String adGroupIds) {
        this.adGroupIds = adGroupIds;
        return this;
    }

    public ArchiveBannerRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }
}
