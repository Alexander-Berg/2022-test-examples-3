package ru.yandex.autotests.direct.cmd.data.campaigns;

import java.util.List;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

public class UnarchiveCampiagnsRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> campaignIds;

    public UnarchiveCampiagnsRequest withCampaingIds(List<Long> campaignIds) {
        this.campaignIds = campaignIds;
        return this;
    }
}
