package ru.yandex.market.cashier.mocks.trust;

import ru.yandex.market.cashier.trust.api.TrustResponseStatus;

public enum ResponseVariable {
    BASKET_ID("%%RND_BALANCE_TRUST_ID%%"),
    PURCHASE_TOKEN("%%RND_BALANCE_TRUST_ID%%"),
    BASKET_STATUS(TrustResponseStatus.success),
    BANK_CARD("500000****0009"),
    TRUST_REFUND_ID("%%RND_BALANCE_TRUST_ID%%");

    Object defaultValue;

    ResponseVariable(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object defaultValue() {
        return defaultValue;
    }
}
