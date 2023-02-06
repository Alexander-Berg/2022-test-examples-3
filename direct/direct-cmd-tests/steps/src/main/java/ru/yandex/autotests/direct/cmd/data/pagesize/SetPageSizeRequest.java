package ru.yandex.autotests.direct.cmd.data.pagesize;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SetPageSizeRequest extends BasicDirectRequest {
    @SerializeKey("subcmd")
    private CMD subCmd;

    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("tab")
    private String tab;

    @SerializeKey("value")
    private String value;

    public CMD getSubCmd() {
        return subCmd;
    }

    public SetPageSizeRequest withSubCmd(CMD subCmd) {
        this.subCmd = subCmd;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public SetPageSizeRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getTab() {
        return tab;
    }

    public SetPageSizeRequest withTab(String tab) {
        this.tab = tab;
        return this;
    }

    public String getValue() {
        return value;
    }

    public SetPageSizeRequest withValue(String value) {
        this.value = value;
        return this;
    }
}
