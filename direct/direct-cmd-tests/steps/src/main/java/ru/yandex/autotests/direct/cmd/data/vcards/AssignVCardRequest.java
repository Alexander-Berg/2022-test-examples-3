package ru.yandex.autotests.direct.cmd.data.vcards;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AssignVCardRequest extends BasicDirectRequest {

    @SerializeKey("bids")
    private String bids;

    @SerializeKey("vcard_id")
    private String vcardId;

    public String getBids() {
        return bids;
    }

    public AssignVCardRequest withBids(String bids) {
        this.bids = bids;
        return this;
    }

    public String getVcardId() {
        return vcardId;
    }

    public AssignVCardRequest withVcardId(String vcardId) {
        this.vcardId = vcardId;
        return this;
    }
}
