package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentOption;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.common.OrderFailure;
import ru.yandex.autotests.market.checkouter.beans.testdata.builders.TestDataMultiOrderBuilder;

import java.util.List;

/**
 * Created by belmatter on 14.10.14.
 */
public class TestDataMultiOrder {
    private Boolean checkedOut;
    private List<TestDataOrder> orders;
    private List<PaymentOption> paymentOptions;
    private List<OrderFailure> orderFailures;
    private Integer buyerRegionId;
    private String buyerCurrency;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private Buyer buyer;
    private boolean isBooked;
    private String promocode;

    public String getPromocode() {
        return promocode;
    }

    public void setPromocode(String promocode) {
        this.promocode = promocode;
    }

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public List<TestDataOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<TestDataOrder> orders) {
        this.orders = orders;
    }

    public List<PaymentOption> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<PaymentOption> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public List<OrderFailure> getOrderFailures() {
        return orderFailures;
    }

    public void setOrderFailures(List<OrderFailure> orderFailures) {
        this.orderFailures = orderFailures;
    }

    public Integer getBuyerRegionId() {
        return buyerRegionId;
    }

    public void setBuyerRegionId(Integer buyerRegionId) {
        this.buyerRegionId = buyerRegionId;
    }

    public String getBuyerCurrency() {
        return buyerCurrency;
    }

    public void setBuyerCurrency(String buyerCurrency) {
        this.buyerCurrency = buyerCurrency;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }

    public boolean isBooked() {
        return isBooked;
    }

    public void setBooked(boolean booked) {
        isBooked = booked;
    }

    public TestDataMultiOrderBuilder but() {
        return new TestDataMultiOrderBuilder().copy(this);
    }

    public TestDataMultiOrder getMultiOrder() {
        return this;
    }
}

