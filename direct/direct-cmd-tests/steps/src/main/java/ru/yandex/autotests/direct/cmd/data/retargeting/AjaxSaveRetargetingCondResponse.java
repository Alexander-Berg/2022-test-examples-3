package ru.yandex.autotests.direct.cmd.data.retargeting;

import com.google.gson.annotations.SerializedName;

public class AjaxSaveRetargetingCondResponse extends CommonAjaxRetConditionResponse {

    @SerializedName("ret_cond_id")
    private Long retCondId;

    public Long getRetCondId() {
        return retCondId;
    }

    public AjaxSaveRetargetingCondResponse withRetCondId(Long retCondId) {
        this.retCondId = retCondId;
        return this;
    }
}
