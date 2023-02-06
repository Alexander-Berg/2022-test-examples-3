package ru.yandex.autotests.direct.cmd.data.autopayment;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxUnbindCardRequest extends BasicDirectRequest {

    @SerializeKey("paymethod_id")
    private String paymethodId;

    @SerializeKey("paymethod_type")
    private String paymethodType;

    public String getPaymethodId() {
        return paymethodId;
    }

    public AjaxUnbindCardRequest withPaymethodId(String paymethodId) {
        this.paymethodId = paymethodId;
        return this;
    }

    public String getPaymethodType() {
        return paymethodType;
    }

    public AjaxUnbindCardRequest withPaymethodType(String paymethodType) {
        this.paymethodType = paymethodType;
        return this;
    }
}
