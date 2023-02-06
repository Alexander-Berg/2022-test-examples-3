package ru.yandex.autotests.direct.cmd.data.autopayment;

import com.google.gson.annotations.SerializedName;

public class PayCard {

    @SerializedName("paymethod_id")
    private String paymethodId;

    @SerializedName("show_number")
    private String showNumber;

    @SerializedName("allowed_set_autopay")
    private String allowedSetAutopay;

    public String getPaymethodId() {
        return paymethodId;
    }

    public PayCard withPaymethodId(String paymethodId) {
        this.paymethodId = paymethodId;
        return this;
    }

    public String getShowNumber() {
        return showNumber;
    }

    public PayCard withShowNumber(String showNumber) {
        this.showNumber = showNumber;
        return this;
    }

    public String getAllowedSetAutopay() {
        return allowedSetAutopay;
    }

    public PayCard withAllowedSetAutopay(String allowedSetAutopay) {
        this.allowedSetAutopay = allowedSetAutopay;
        return this;
    }
}
