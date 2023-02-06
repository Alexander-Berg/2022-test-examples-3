package ru.yandex.market.checkout.helpers.utils;

import ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

public class PaymentParameters {

    public static final PaymentParameters DEFAULT = new PaymentParameters();
    public static final String DEFAULT_RETURN_PATH = "http://localhost/!!ORDER_ID!!";

    private long uid = BuyerProvider.UID;
    private boolean sandbox = false;

    private PaymentFormType paymentFormType = null;
    private String returnPath = DEFAULT_RETURN_PATH;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public PaymentFormType getPaymentFormType() {
        return paymentFormType;
    }

    public void setPaymentFormType(PaymentFormType paymentFormType) {
        this.paymentFormType = paymentFormType;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }
}
