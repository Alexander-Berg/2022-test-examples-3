package ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxSetAutoResourcesRequest extends BasicDirectRequest{
    @SerializeKey("cid")
    private String cid;

    @SerializeKey("action")
    private String action;

    public String getCid() {
        return cid;
    }

    public AjaxSetAutoResourcesRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getAction() {
        return action;
    }

    public AjaxSetAutoResourcesRequest withAction(String action) {
        this.action = action;
        return this;
    }
}
