package ru.yandex.autotests.direct.cmd.data.autopayment;

public enum WalletPaymentTransactionsStatusEnum {
    PAYMENT_NOT_FOUND("payment_not_found"),
    WRONG_SMS_CODE("wrong_sms_code"),
    INVALID_TOKEN("invalid_token"),
    ALREADY_PURCHASED("already_purchased"),
    ALREADY_PENDING("already_pending"),
    PAYMENT_TIMEOUT("payment_timeout"),
    NOT_ENOUGH_FUNDS("not_enough_funds"),
    AUTHORIZATION_REJECT("authorization_reject"),
    PAYMENT_REFUSED("payment_refused"),
    TECHNICAL_ERROR("technical_error"),
    EXPIRED_CARD("expired_card"),
    LIMIT_EXCEEDED("limit_exceeded");

    private String value;

    WalletPaymentTransactionsStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
