package ru.yandex.autotests.direct.cmd.data.showcamp;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class DeleteCampRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private String cid;

    public String getCid() {
        return cid;
    }

    public DeleteCampRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }
}
