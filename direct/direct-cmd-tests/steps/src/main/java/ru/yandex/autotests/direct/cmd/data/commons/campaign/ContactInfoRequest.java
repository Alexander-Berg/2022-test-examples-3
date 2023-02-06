package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ContactInfoRequest extends BasicDirectRequest{
    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("bid")
    private Long bid;

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }
}
