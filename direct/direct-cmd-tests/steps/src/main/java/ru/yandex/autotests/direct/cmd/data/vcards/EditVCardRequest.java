package ru.yandex.autotests.direct.cmd.data.vcards;


import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class EditVCardRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long campaignId;

    @SerializeKey("bid")
    private Long bannerId;

    @SerializeKey("vcard_id")
    private Long vCardId;

    public Long getCampaignId() {
        return campaignId;
    }

    public EditVCardRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public Long getBannerId() {
        return bannerId;
    }

    public EditVCardRequest withBannerId(Long bannerId) {
        this.bannerId = bannerId;
        return this;
    }

    public Long getVCardId() {
        return vCardId;
    }

    public EditVCardRequest withVCardId(Long vCardId) {
        this.vCardId = vCardId;
        return this;
    }
}
