package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.Context;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.orders.certificates.Certificate;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataDelivery;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;

import java.util.List;

public class BlueOrderParameters {
    private int region;
    private Buyer buyer;
    private List<TestDataItem> itemsForOrder;
    private TestDataDelivery delivery;
    private String promoCode;
    private List<Long> coinIdsToUse;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private Status expectedStatus;
    private long shopId;
    private long supplierId;
    private Context context;
    private Certificate certificate;

    public BlueOrderParameters(long shopId) {
        this.shopId = shopId;
    }

    public BlueOrderParameters(long shopId, long supplierId) {
        this.shopId = shopId;
        this.supplierId = supplierId;
    }

    public int getRegion() {
        return region;
    }

    public BlueOrderParameters setRegion(int region) {
        this.region = region;
        return this;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public BlueOrderParameters setBuyer(Buyer buyer) {
        this.buyer = buyer;
        return this;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public BlueOrderParameters setPromoCode(String promoCode) {
        this.promoCode = promoCode;
        return this;
    }

    public List<Long> getCoinIdsToUse() {
        return coinIdsToUse;
    }

    public BlueOrderParameters setCoinIdsToUse(List<Long> coinIdsToUse) {
        this.coinIdsToUse = coinIdsToUse;
        return this;
    }

    public List<TestDataItem> getItemsForOrder() {
        return itemsForOrder;
    }

    public BlueOrderParameters setItemsForOrder(List<TestDataItem> itemsForOrder) {
        this.itemsForOrder = itemsForOrder;
        return this;
    }

    public TestDataDelivery getDelivery() {
        return delivery;
    }

    public BlueOrderParameters setDelivery(TestDataDelivery delivery) {
        this.delivery = delivery;
        return this;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public BlueOrderParameters setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BlueOrderParameters setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public Status getExpectedStatus() {
        return expectedStatus;
    }

    public BlueOrderParameters setExpectedStatus(Status expectedStatus) {
        this.expectedStatus = expectedStatus;
        return this;
    }

    public long getShopId() {
        return shopId;
    }

    public long getSupplierId() {
        return supplierId;
    }

    public Context getContext() {
        return context;
    }

    public BlueOrderParameters setContext(Context context) {
        this.context = context;
        return this;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public BlueOrderParameters setCertificate(Certificate certificate) {
        this.certificate = certificate;
        return this;
    }

    @Override
    public String toString() {
        return "BlueOrderParameters{" +
                "region=" + region +
                ", buyer=" + buyer +
                ", itemsForOrder=" + itemsForOrder +
                ", delivery=" + delivery +
                ", paymentType=" + paymentType +
                ", paymentMethod=" + paymentMethod +
                ", expectedStatus=" + expectedStatus +
                ", shopId=" + shopId +
                ", context=" + context +
                ", certificate=" + certificate +
                '}';
    }
}
