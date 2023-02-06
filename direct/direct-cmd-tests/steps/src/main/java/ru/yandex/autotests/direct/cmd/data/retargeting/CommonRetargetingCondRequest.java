package ru.yandex.autotests.direct.cmd.data.retargeting;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CommonRetargetingCondRequest extends BasicDirectRequest {

    @SerializeKey("ret_cond_id")
    private String retCondId;

    public String getRetCondId() {
        return retCondId;
    }

    public CommonRetargetingCondRequest withRetCondId(String retCondId) {
        this.retCondId = retCondId;
        return this;
    }
}
