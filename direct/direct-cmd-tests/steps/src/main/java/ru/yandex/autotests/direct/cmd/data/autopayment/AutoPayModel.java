package ru.yandex.autotests.direct.cmd.data.autopayment;

import com.google.gson.annotations.SerializedName;

public class AutoPayModel {

    @SerializedName("autopay_mode")
    private String autopayMode;

    @SerializedName("paymethod_type")
    private String paymethodType;

    @SerializedName("paymethod_id")
    private String paymethodId;

    @SerializedName("remaining_sum")
    private String remainingSum;

    @SerializedName("payment_sum")
    private String paymentSum;

    public String getAutopayMode() {
        return autopayMode;
    }

    public AutoPayModel withAutopayMode(String autopayMode) {
        this.autopayMode = autopayMode;
        return this;
    }

    public String getPaymethodType() {
        return paymethodType;
    }

    public AutoPayModel withPaymethodType(String paymethodType) {
        this.paymethodType = paymethodType;
        return this;
    }

    public String getPaymethodId() {
        return paymethodId;
    }

    public AutoPayModel withPaymethodId(String paymethodId) {
        this.paymethodId = paymethodId;
        return this;
    }

    public String getRemainingSum() {
        return remainingSum;
    }

    public AutoPayModel withRemainingSum(String remainingSum) {
        this.remainingSum = remainingSum;
        return this;
    }

    public String getPaymentSum() {
        return paymentSum;
    }

    public AutoPayModel withPaymentSum(String paymentSum) {
        this.paymentSum = paymentSum;
        return this;
    }
}
