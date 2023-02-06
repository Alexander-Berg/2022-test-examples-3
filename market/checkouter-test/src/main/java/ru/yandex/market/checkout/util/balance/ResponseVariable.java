package ru.yandex.market.checkout.util.balance;

import ru.yandex.market.checkout.checkouter.balance.BasketStatus;

public enum ResponseVariable {
    ORDER_ID("%%RND_POS_INT_ID%%"),
    BASKET_ID("%%RND_BALANCE_TRUST_ID%%"),
    PURCHASE_TOKEN("%%RND_PURCHASE_TOKEN%%"),
    BASKET_STATUS(BasketStatus.success),
    BANK_CARD("500000****0009"),
    TRUST_REFUND_ID("%%RND_BALANCE_TRUST_ID%%"),
    ORDER_ITEM_1("{{ORDER_ITEM_1}}"),
    ORDER_ITEM_2("{{ORDER_ITEM_2}}"),
    ORDER_ITEM_3("{{ORDER_ITEM_3}}"),
    ORDER_DELIVERY("{{ORDER_DELIVERY}}"),
    PERSON_ID("{{PERSON_ID}}"),
    CLIENT_ID("5835538"),
    AMOUNT("250.00"),
    PASSPORT_ID("359953025"),
    ORDERS_CONTENT("{{ORDERS_CONTENT}}"),
    REFUNDS_CONTENT("{{REFUNDS_CONTENT}}");

    Object defaultValue;

    ResponseVariable(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object defaultValue() {
        return defaultValue;
    }
}
