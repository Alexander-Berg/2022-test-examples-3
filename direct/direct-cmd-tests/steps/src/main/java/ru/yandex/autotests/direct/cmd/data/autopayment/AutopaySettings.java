package ru.yandex.autotests.direct.cmd.data.autopayment;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AutopaySettings {

    @SerializedName("wallet_cid")
    private String walletCid;

    @SerializedName("paymethod_type")
    private String payMethodType;

    @SerializedName("paymethod_id")
    private String payMethodId;

    @SerializedName("remaining_sum")
    private String remainingSum;

    @SerializedName("payment_sum")
    private String paymentSum;

    @SerializedName("cards")
    private List<PayCard> cards;

    @SerializedName("yandex_moneys")
    private List<PayCard> yandexMoneys;

    @SerializedName("error")
    private String error;

    public String getWalletCid() {
        return walletCid;
    }

    public AutopaySettings withWalletCid(String walletCid) {
        this.walletCid = walletCid;
        return this;
    }

    public String getPayMethodType() {
        return payMethodType;
    }

    public AutopaySettings withPayMethodType(String payMethodType) {
        this.payMethodType = payMethodType;
        return this;
    }

    public String getPayMethodId() {
        return payMethodId;
    }

    public AutopaySettings withPayMethodId(String payMethodId) {
        this.payMethodId = payMethodId;
        return this;
    }

    public String getRemainingSum() {
        return remainingSum;
    }

    public AutopaySettings withRemainingSum(String remainingSum) {
        this.remainingSum = remainingSum;
        return this;
    }

    public String getPaymentSum() {
        return paymentSum;
    }

    public AutopaySettings withPaymentSum(String paymentSum) {
        this.paymentSum = paymentSum;
        return this;
    }

    public List<PayCard> getCards() {
        return cards;
    }

    public AutopaySettings withCards(List<PayCard> cards) {
        this.cards = cards;
        return this;
    }

    public List<PayCard> getYandexMoneys() {
        return yandexMoneys;
    }

    public AutopaySettings withYandexMoneys(List<PayCard> yandexMoneys) {
        this.yandexMoneys = yandexMoneys;
        return this;
    }

    public String getError() {
        return error;
    }

    public AutopaySettings withError(String error) {
        this.error = error;
        return this;
    }
}