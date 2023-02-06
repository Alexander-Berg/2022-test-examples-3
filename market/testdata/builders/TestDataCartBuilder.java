package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.Changes;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentOption;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataCart;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataDelivery;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;

import java.util.Arrays;
import java.util.List;

public class TestDataCartBuilder implements Cloneable {

    protected Long shopId;
    protected List<TestDataItem> items;
    protected List<TestDataDelivery> deliveryOptions;
    protected List<PaymentOption> paymentOptions;
    protected TestDataDelivery delivery;
    protected String currency;
    protected Changes changes;
    protected List<PaymentMethod> paymentMethods;
    protected List<Long> coinIdsToUse;

    public TestDataCartBuilder withShopId(Long value) {
        this.shopId = value;
        return this;
    }

    public TestDataCartBuilder withItems(List<TestDataItem> value) {
        this.items = value;
        return this;
    }

    public TestDataCartBuilder withDeliveryOptions(List<TestDataDelivery> value) {
        this.deliveryOptions = value;
        return this;
    }

    public TestDataCartBuilder withPaymentOptions(List<PaymentOption> value) {
        this.paymentOptions = value;
        return this;
    }

    public TestDataCartBuilder withCoinIdsToUse(List<Long> coins){
        this.coinIdsToUse = coins;
        return this;
    }

    public TestDataCartBuilder withDelivery(TestDataDelivery value) {
        this.delivery = value;
        return this;
    }

    public TestDataCartBuilder withCurrency(String value) {
        this.currency = value;
        return this;
    }

    public TestDataCartBuilder withChanges(Changes value) {
        this.changes = value;
        return this;
    }

    public TestDataCartBuilder withPaymentMethods(List<PaymentMethod> value) {
        this.paymentMethods = value;
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

    public TestDataCartBuilder but() {
        return (TestDataCartBuilder) clone();
    }

    public TestDataCartBuilder copy(TestDataCart cart) {
        withShopId(cart.getShopId());
        withItems(cart.getItems());
        withDeliveryOptions(cart.getDeliveryOptions());
        withPaymentOptions(cart.getPaymentOptions());
        withDelivery(cart.getDelivery());
        withCurrency(cart.getCurrency());
        withChanges(cart.getChanges());
        withPaymentMethods(cart.getPaymentMethods());
        withCoinIdsToUse(cart.getCoinIdsToUse());
        return this;
    }


    public TestDataCart build() {
        try {
            TestDataCart result = new TestDataCart();
            result.setShopId(shopId);
            result.setItems(items);
            result.setDeliveryOptions(deliveryOptions);
            result.setPaymentOptions(paymentOptions);
            result.setDelivery(delivery);
            result.setCurrency(currency);
            result.setChanges(changes);
            result.setPaymentMethods(paymentMethods);
            result.setCoinIdsToUse(coinIdsToUse);
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

    public TestDataCartBuilder withItems(TestDataItem... items) {
        return withItems(Arrays.asList(items));
    }

    public TestDataCartBuilder withDeliveryOptions(TestDataDelivery... deliveryOptions) {
        return withDeliveryOptions(Arrays.asList(deliveryOptions));
    }

    public TestDataCartBuilder withPaymentMethods(PaymentMethod... paymentMethods) {
        return withPaymentMethods(Arrays.asList(paymentMethods));
    }

}
