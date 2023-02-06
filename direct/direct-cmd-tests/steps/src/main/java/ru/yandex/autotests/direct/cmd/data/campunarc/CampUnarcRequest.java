package ru.yandex.autotests.direct.cmd.data.campunarc;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CampUnarcRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("tab")
    private String tab;

    public String getCid() {
        return cid;
    }

    public CampUnarcRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public CampUnarcRequest withTab(String tab) {
        this.tab = tab;
        return this;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }
}
