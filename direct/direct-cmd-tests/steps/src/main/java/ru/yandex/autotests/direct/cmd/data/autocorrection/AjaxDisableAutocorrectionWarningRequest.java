package ru.yandex.autotests.direct.cmd.data.autocorrection;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxDisableAutocorrectionWarningRequest extends BasicDirectRequest {

    @SerializeKey("bid")
    private String bid;

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }
}
