package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class RemoderateCampRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long cid;

    public Long getCid() {
        return cid;
    }

    public RemoderateCampRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }
}
