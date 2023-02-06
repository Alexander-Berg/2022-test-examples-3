package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class OrderCampRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("accept")
    private String accept;

    @SerializeKey("agree")
    private String agree;

    public Long getCid() {
        return cid;
    }

    public OrderCampRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getAccept() {
        return accept;
    }

    public OrderCampRequest withAccept(String accept) {
        this.accept = accept;
        return this;
    }

    public String getAgree() {
        return agree;
    }

    public OrderCampRequest withAgree(String agree) {
        this.agree = agree;
        return this;
    }
}
