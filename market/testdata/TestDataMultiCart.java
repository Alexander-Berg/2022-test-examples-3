package ru.yandex.autotests.market.checkouter.beans.testdata;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.common.CartFailure;
import ru.yandex.autotests.market.checkouter.beans.testdata.builders.TestDataMultiCartBuilder;

import java.util.List;

/**
 * Created by belmatter on 10.10.14.
 */
public class TestDataMultiCart {

    private Integer buyerRegionId;

    private String buyerCurrency;

    private PaymentMethod paymentMethod;
    private PaymentType paymentType;

    private List<TestDataCart> carts;

    private List<CartFailure> cartFailures;

    private Boolean isBooked;

    private String promocode;

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

    public List<TestDataCart> getCarts() {
        return carts;
    }

    public void setCarts(List<TestDataCart> carts) {
        this.carts = carts;
    }

    public List<CartFailure> getCartFailures() {
        return cartFailures;
    }

    public void setCartFailures(List<CartFailure> cartFailures) {
        this.cartFailures = cartFailures;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getBooked() {
        return isBooked;
    }

    public void setBooked(Boolean booked) {
        isBooked = booked;
    }

    public TestDataMultiCartBuilder but() {
        return new TestDataMultiCartBuilder().copy(this);
    }

    public TestDataMultiCart getMultiCart() {
        return this;
    }

    public String getPromocode() {
        return promocode;
    }

    public void setPromocode(String promocode) {
        this.promocode = promocode;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
