package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentOption;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.common.OrderFailure;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataDelivery;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataMultiOrder;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;

import java.util.Arrays;
import java.util.List;

public class TestDataMultiOrderBuilder implements Cloneable {

    protected TestDataMultiOrderBuilder self;

    protected Boolean checkedOut;
    private boolean isSetCheckedOut;

    protected List<TestDataOrder> orders;
    private boolean isSetOrders;

    protected List<PaymentOption> paymentOptions;
    private boolean isSetPaymentOptions;

    private List<OrderFailure> orderFailures;
    private boolean isSetOrderFailures;

    protected Integer buyerRegionId;
    private boolean isSetBuyerRegionId;

    protected String buyerCurrency;
    private boolean isSetBuyerCurrency;

    protected PaymentType paymentType;
    private boolean isSetPaymentType;

    protected PaymentMethod paymentMethod;
    private boolean isSetPaymentMethod;

    protected Buyer buyer;
    private boolean isSetBuyer;

    protected boolean booked;
    private boolean isSetBooked;

    private String promoCode;

    public TestDataMultiOrderBuilder() {
        self = this;
    }


    public TestDataMultiOrderBuilder withCheckedOut(Boolean value) {
        this.checkedOut = value;
        this.isSetCheckedOut = true;
        return self;
    }

    public TestDataMultiOrderBuilder withOrders(List<TestDataOrder> value) {
        this.orders = value;
        this.isSetOrders = true;
        return self;
    }

    public TestDataMultiOrderBuilder withPromoCode(String promoCode) {
        this.promoCode = promoCode;
        return self;
    }

    public TestDataMultiOrderBuilder withPaymentOptions(List<PaymentOption> value) {
        this.paymentOptions = value;
        this.isSetPaymentOptions = true;
        return self;
    }

    public TestDataMultiOrderBuilder withOrderFailures(List<OrderFailure> value) {
        this.orderFailures = value;
        this.isSetOrderFailures = true;
        return self;
    }

    public TestDataMultiOrderBuilder withBuyerRegionId(Integer value) {
        this.buyerRegionId = value;
        this.isSetBuyerRegionId = true;
        return self;
    }

    public TestDataMultiOrderBuilder withBuyerCurrency(String value) {
        this.buyerCurrency = value;
        this.isSetBuyerCurrency = true;
        return self;
    }

    public TestDataMultiOrderBuilder withPaymentType(PaymentType value) {
        this.paymentType = value;
        this.isSetPaymentType = true;
        return self;
    }

    public TestDataMultiOrderBuilder withPaymentMethod(PaymentMethod value) {
        this.paymentMethod = value;
        this.isSetPaymentMethod = true;
        return self;
    }

    public TestDataMultiOrderBuilder withBuyer(Buyer value) {
        this.buyer = value;
        this.isSetBuyer = true;
        return self;
    }

    public TestDataMultiOrderBuilder withBooked(boolean value) {
        this.booked = value;
        this.isSetBooked = true;
        return self;
    }

    @Override
    public Object clone() {
        try {
            TestDataMultiOrderBuilder result = (TestDataMultiOrderBuilder) super.clone();
            result.self = result;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    public TestDataMultiOrderBuilder but() {
        return (TestDataMultiOrderBuilder) clone();
    }

    public TestDataMultiOrderBuilder copy(TestDataMultiOrder order) {
        withCheckedOut(order.getCheckedOut());
        withOrders(order.getOrders());
        withPaymentOptions(order.getPaymentOptions());
        withOrderFailures(order.getOrderFailures());
        withBuyerRegionId(order.getBuyerRegionId());
        withBuyerCurrency(order.getBuyerCurrency());
        withPaymentType(order.getPaymentType());
        withPaymentMethod(order.getPaymentMethod());
        withBuyer(order.getBuyer());
        withBooked(order.isBooked());
        withPromoCode(order.getPromocode());
        return self;
    }

    public TestDataMultiOrder build() {
        try {
            TestDataMultiOrder result = new TestDataMultiOrder();
            if (isSetCheckedOut) {
                result.setCheckedOut(checkedOut);
            }
            if (isSetOrders) {
                result.setOrders(orders);
            }
            if (isSetPaymentOptions) {
                result.setPaymentOptions(paymentOptions);
            }
            if (isSetOrderFailures) {
                result.setOrderFailures(orderFailures);
            }
            if (isSetBuyerRegionId) {
                result.setBuyerRegionId(buyerRegionId);
            }
            if (isSetBuyerCurrency) {
                result.setBuyerCurrency(buyerCurrency);
            }
            if (isSetPaymentType) {
                result.setPaymentType(paymentType);
            }
            if (isSetPaymentMethod) {
                result.setPaymentMethod(paymentMethod);
            }
            if (isSetBuyer) {
                result.setBuyer(buyer);
            }
            if (isSetBooked) {
                result.setBooked(booked);
            }
            result.setPromocode(promoCode);
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

    public TestDataMultiOrderBuilder withOrders(TestDataOrder order) {
        return this.withOrders(Arrays.asList(order));
    }

    public TestDataMultiOrderBuilder withFirstOrderDelivery(TestDataDelivery delivery) {
        if (!isSetOrders || orders.isEmpty()) {
            throw new IllegalStateException("Can not get first order, when orders list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        TestDataOrder order = orders.get(0);
        order.setDelivery(delivery);
        return withOrders(orders);
    }

    public TestDataMultiOrderBuilder withFirstItemCount(int count) {
        if (!isSetOrders || orders.isEmpty()) {
            throw new IllegalStateException("Can not get first order, when orders list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        List<TestDataItem> items = orders.get(0).getItems();
        if (items.isEmpty()) {
            throw new IllegalStateException("Can not get items in first order, when items list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        TestDataItem item = items.get(0);
        item.setCount(count);
        return withOrders(orders);
    }

    public TestDataMultiOrderBuilder withFirstItemPrice(Double price) {
        if (!isSetOrders || orders.isEmpty()) {
            throw new IllegalStateException("Can not get first order, when orders list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        List<TestDataItem> items = orders.get(0).getItems();
        if (items.isEmpty()) {
            throw new IllegalStateException("Can not get items in first order, when items list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        TestDataItem item = items.get(0);
        item.setPrice(price);
        return withOrders(orders);
    }

    public TestDataMultiOrderBuilder withFirstItemBuyerPrice(Double buyerPrice) {
        if (!isSetOrders || orders.isEmpty()) {
            throw new IllegalStateException("Can not get first order, when orders list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        List<TestDataItem> items = orders.get(0).getItems();
        if (items.isEmpty()) {
            throw new IllegalStateException("Can not get items in first order, when items list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        TestDataItem item = items.get(0);
        item.setBuyerPrice(buyerPrice);
        return withOrders(orders);
    }


    public TestDataMultiOrderBuilder withFirstFeeSum(Double feeSum) {
        if (!isSetOrders || orders.isEmpty()) {
            throw new IllegalStateException("Can not get first order, when orders list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        List<TestDataItem> items = orders.get(0).getItems();
        if (items.isEmpty()) {
            throw new IllegalStateException("Can not get items in first order, when items list is null or empty. " +
                    "Multiorder : \n" + this);
        }
        TestDataItem item = items.get(0);
        item.setFeeSum(feeSum);
        return withOrders(orders);
    }

}
