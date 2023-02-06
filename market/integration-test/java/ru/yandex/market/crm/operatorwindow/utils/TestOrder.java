package ru.yandex.market.crm.operatorwindow.utils;

import ru.yandex.market.crm.util.Randoms;

public class TestOrder {

    public static final Long TEST_ORDER_NUMBER = Randoms.positiveLongValue();
    public static final String TEST_BUYER_EMAIL = Randoms.email();
    public static final String TEST_BUYER_PHONE = Randoms.phoneNumber();
    public static final String TEST_BUYER_FULL_NAME = Randoms.string();

    private boolean dropshipping;
    private String paymentMethod;
    private String paymentType;
    private String buyerEmail = TEST_BUYER_EMAIL;
    private String buyerPhone = TEST_BUYER_PHONE;
    private String buyerFullName = TEST_BUYER_FULL_NAME;
    private String status;
    private String substatus;

    public boolean isDropshipping() {
        return dropshipping;
    }

    public TestOrder setDropshipping(boolean dropshipping) {
        this.dropshipping = dropshipping;
        return this;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public TestOrder setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public TestOrder setPaymentType(String paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public TestOrder setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
        return this;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public TestOrder setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
        return this;
    }

    public String getBuyerFullName() {
        return buyerFullName;
    }

    public TestOrder setBuyerFullName(String buyerFullName) {
        this.buyerFullName = buyerFullName;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public TestOrder setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getSubstatus() {
        return substatus;
    }

    public TestOrder setSubstatus(String substatus) {
        this.substatus = substatus;
        return this;
    }
}
