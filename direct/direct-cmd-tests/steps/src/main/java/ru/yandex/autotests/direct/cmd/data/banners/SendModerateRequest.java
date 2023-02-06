package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SendModerateRequest extends BasicDirectRequest {
    @SerializeKey("bid")
    private Long bannerId;

    @SerializeKey("cid")
    private Long campaignId;

    @SerializeKey("adgroup_ids")
    private Long groupId;


    public Long getBannerId() {
        return bannerId;
    }

    public SendModerateRequest withBannerId(Long bannerId) {
        this.bannerId = bannerId;
        return this;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public SendModerateRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public Long getGroupId() {
        return groupId;
    }

    public SendModerateRequest withGroupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }
}
