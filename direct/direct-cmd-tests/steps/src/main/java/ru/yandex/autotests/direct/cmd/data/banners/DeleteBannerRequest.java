package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class DeleteBannerRequest extends BasicDirectRequest {

    @SerializeKey("adgroup_ids")
    private String adgroupIds;

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("bid")
    private String bid;

    @SerializeKey("delete_whole_group")
    private String deleteWholeGroup;

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public DeleteBannerRequest withAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public DeleteBannerRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getBid() {
        return bid;
    }

    public DeleteBannerRequest withBid(String bid) {
        this.bid = bid;
        return this;
    }

    public String getDeleteWholeGroup() {
        return deleteWholeGroup;
    }

    public DeleteBannerRequest withDeleteWholeGroup(String deleteWholeGroup) {
        this.deleteWholeGroup = deleteWholeGroup;
        return this;
    }
}
