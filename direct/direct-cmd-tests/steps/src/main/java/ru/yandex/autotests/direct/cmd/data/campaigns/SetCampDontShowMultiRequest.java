package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

import java.util.List;

public class SetCampDontShowMultiRequest extends BasicDirectRequest {

    public enum Operation {
        DISABLE, ENABLE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    @SerializeKey("op")
    private Operation op;

    @SerializeKey("cid")
    private Long campaignId;

    @SerializeKey("pages_checked")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<String> pagesChecked;

    public Long getCampaignId() {
        return campaignId;
    }

    public SetCampDontShowMultiRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public Operation getOp() {
        return op;
    }

    public SetCampDontShowMultiRequest withOp(Operation op) {
        this.op = op;
        return this;
    }

    public List<String> getPagesChecked() {
        return pagesChecked;
    }

    public SetCampDontShowMultiRequest withPagesChecked(List<String> pagesChecked) {
        this.pagesChecked = pagesChecked;
        return this;
    }
}
