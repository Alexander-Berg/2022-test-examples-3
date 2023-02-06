package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.HashMap;
import java.util.Map;

public class CheckoutRequestConfiguration {

    private boolean reserveOnly;
    private String asyncPaymentCardId;
    private String loginId;
    private Map<String, String> headers = new HashMap<>();

    public boolean isReserveOnly() {
        return reserveOnly;
    }

    public void setReserveOnly(boolean reserveOnly) {
        this.reserveOnly = reserveOnly;
    }

    public String getAsyncPaymentCardId() {
        return asyncPaymentCardId;
    }

    public void setAsyncPaymentCardId(String asyncPaymentCardId) {
        this.asyncPaymentCardId = asyncPaymentCardId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }
}
