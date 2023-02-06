package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ChangeFlagsAjaxRequest extends BasicDirectRequest {
    @SerializeKey("bid")
    private String bid;

    @SerializeKey("flag")
    private String flag;

    public String getBid() {
        return bid;
    }

    public ChangeFlagsAjaxRequest withBid(String bid) {
        this.bid = bid;
        return this;
    }

    public ChangeFlagsAjaxRequest addFlag(AdWarningFlag flag) {
        this.flag = flag.toString() + "=1";
        return this;
    }

    public ChangeFlagsAjaxRequest removeFlag(AdWarningFlag flag) {
        this.flag = flag.toString() + "=-1";
        return this;
    }
}
