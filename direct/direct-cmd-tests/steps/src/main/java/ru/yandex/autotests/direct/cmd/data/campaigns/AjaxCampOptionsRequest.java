package ru.yandex.autotests.direct.cmd.data.campaigns;
// Task: TESTIRT-9409.

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxCampOptionsRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("offline_price_editor")
    private OfflinePriceEditor offlinePriceEditor;

    public OfflinePriceEditor getOfflinePriceEditor() {
        return offlinePriceEditor;
    }

    public AjaxCampOptionsRequest withOfflinePriceEditor(OfflinePriceEditor offlinePriceEditor) {
        this.offlinePriceEditor = offlinePriceEditor;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public AjaxCampOptionsRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

}
