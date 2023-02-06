package ru.yandex.autotests.direct.cmd.data.excel;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/*
* todo javadoc
*/
public class ConfirmSaveCampRequest extends BasicDirectRequest {

    @SerializeKey("svars_name")
    private String sVarsName;

    @SerializeKey("confirm")
    private Boolean confirm;

    @SerializeKey("cid")
    private String cid;

    public String getsVarsName() {
        return sVarsName;
    }

    public ConfirmSaveCampRequest withsVarsName(String sVarsName) {
        this.sVarsName = sVarsName;
        return this;
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public ConfirmSaveCampRequest withConfirm(Boolean confirm) {
        this.confirm = confirm;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public ConfirmSaveCampRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }
}
