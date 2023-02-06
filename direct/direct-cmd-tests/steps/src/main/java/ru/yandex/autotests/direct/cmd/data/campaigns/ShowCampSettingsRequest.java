package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ShowCampSettingsRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long campaignId;

    public Long getCampaignId() {
        return campaignId;
    }

    public ShowCampSettingsRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }
}
