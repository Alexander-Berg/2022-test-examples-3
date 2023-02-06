package ru.yandex.autotests.direct.cmd.data.campaigns;

import java.util.List;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

public class DeleteCampaignsRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> campaignIds;

    public List<Long> getCampaignIds() {
        return campaignIds;
    }

    public void setCampaignIds(List<Long> campaignIds) {
        this.campaignIds = campaignIds;
    }

    public DeleteCampaignsRequest withCampaignIds(List<Long> campaignIds) {
        this.campaignIds = campaignIds;
        return this;
    }
}
