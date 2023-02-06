package ru.yandex.autotests.market.checkouter.beans.testdata;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import ru.yandex.autotests.market.checkouter.beans.Changes;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentOption;
import ru.yandex.autotests.market.checkouter.beans.testdata.builders.TestDataCartBuilder;

import java.util.List;

/**
 * Created by belmatter on 10.10.14.
 */
public class TestDataCart {

    private Long shopId;

    private List<TestDataItem> items;

    private List<TestDataDelivery> deliveryOptions;

    private List<PaymentOption> paymentOptions;

    private List<PaymentMethod> paymentMethods;

    private TestDataDelivery delivery;

    private Changes changes;

    private String currency;

    private List<Long> coinIdsToUse;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public List<TestDataItem> getItems() {
        return items;
    }

    public void setItems(List<TestDataItem> items) {
        this.items = items;
    }

    public List<TestDataDelivery> getDeliveryOptions() {
        return deliveryOptions;
    }

    public void setDeliveryOptions(List<TestDataDelivery> deliveryOptions) {
        this.deliveryOptions = deliveryOptions;
    }

    public List<PaymentOption> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<PaymentOption> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public TestDataDelivery getDelivery() {
        return delivery;
    }

    public void setDelivery(TestDataDelivery delivery) {
        this.delivery = delivery;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Changes getChanges() {
        return changes;
    }

    public void setChanges(Changes changes) {
        this.changes = changes;
    }

    public TestDataCartBuilder but() {
        return new TestDataCartBuilder().copy(this);
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public List<Long> getCoinIdsToUse() {
        return coinIdsToUse;
    }

    public void setCoinIdsToUse(List<Long> coinIdsToUse) {
        this.coinIdsToUse = coinIdsToUse;
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
