package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class EditCampRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long campaignId;

    public Long getCampaignId() {
        return campaignId;
    }

    public EditCampRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }
}
