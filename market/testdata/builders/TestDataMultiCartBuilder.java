package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.common.CartFailure;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataCart;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataMultiCart;

import java.util.Arrays;
import java.util.List;

public class TestDataMultiCartBuilder implements Cloneable {

    protected Integer buyerRegionId;
    protected String buyerCurrency;
    protected List<TestDataCart> carts;
    protected List<CartFailure> cartFailures;
    protected PaymentMethod paymentMethod;
    protected PaymentType paymentType;
    protected String promocode;

    protected Boolean booked;

    public TestDataMultiCartBuilder withBuyerRegionId(Integer value) {
        this.buyerRegionId = value;
        return this;
    }

    public TestDataMultiCartBuilder withBuyerCurrency(String value) {
        this.buyerCurrency = value;
        return this;
    }

    public TestDataMultiCartBuilder withCarts(TestDataCart... value) {
        return this.withCarts(Arrays.asList(value));
    }

    public TestDataMultiCartBuilder withCarts(List<TestDataCart> value) {
        this.carts = value;
        return this;
    }

    public TestDataMultiCartBuilder withCartFailures(List<CartFailure> value) {
        this.cartFailures = value;
        return this;
    }

    public TestDataMultiCartBuilder withPaymentMethod(PaymentMethod value) {
        this.paymentMethod = value;
        return this;
    }

    public TestDataMultiCartBuilder withPaymentType(PaymentType value) {
        this.paymentType = value;
        return this;
    }

    public TestDataMultiCartBuilder withPromocode(String value) {
        this.promocode = value;
        return this;
    }

    public TestDataMultiCartBuilder withBooked(Boolean value) {
        this.booked = value;
        return this;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    public TestDataMultiCartBuilder but() {
        return (TestDataMultiCartBuilder) clone();
    }

    public TestDataMultiCartBuilder copy(TestDataMultiCart multiCart) {
        withBuyerRegionId(multiCart.getBuyerRegionId());
        withBuyerCurrency(multiCart.getBuyerCurrency());
        withCarts(multiCart.getCarts());
        withCartFailures(multiCart.getCartFailures());
        withPaymentMethod(multiCart.getPaymentMethod());
        withBooked(multiCart.getBooked());
        withPaymentType(multiCart.getPaymentType());
        withPromocode(multiCart.getPromocode());
        return this;
    }

    /**
     * Creates a new {@link TestDataMultiCart} based on this builder's settings.
     *
     * @return the created TestDataMultiCart
     */
    public TestDataMultiCart build() {
        try {
            TestDataMultiCart result = new TestDataMultiCart();
            result.setBuyerRegionId(buyerRegionId);
            result.setBuyerCurrency(buyerCurrency);
            result.setCarts(carts);
            result.setCartFailures(cartFailures);
            result.setPaymentMethod(paymentMethod);
            result.setBooked(booked);
            result.setPaymentType(paymentType);
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }
}
