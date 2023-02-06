package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.Context;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.Status;

import java.util.List;

public class BlueMultiOrderParameters {
    private int region;
    private Buyer buyer;
    private String promoCode;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private Status expectedStatus;
    private Context context;
    private List<BlueOrderParameters> ordersParameters;

    public int getRegion() {
        return region;
    }

    public BlueMultiOrderParameters setRegion(int region) {
        this.region = region;
        return this;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public BlueMultiOrderParameters setBuyer(Buyer buyer) {
        this.buyer = buyer;
        return this;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public BlueMultiOrderParameters setPromoCode(String promoCode) {
        this.promoCode = promoCode;
        return this;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public BlueMultiOrderParameters setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BlueMultiOrderParameters setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public Status getExpectedStatus() {
        return expectedStatus;
    }

    public BlueMultiOrderParameters setExpectedStatus(Status expectedStatus) {
        this.expectedStatus = expectedStatus;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public BlueMultiOrderParameters setContext(Context context) {
        this.context = context;
        return this;
    }

    public List<BlueOrderParameters> getOrdersParameters() {
        return ordersParameters;
    }

    public BlueMultiOrderParameters setBlueMultiOrderParameters(List<BlueOrderParameters> ordersParameters) {
        this.ordersParameters = ordersParameters;
        return this;
    }

    @Override
    public String toString() {
        return "BlueMultiOrderParameters{" +
                "region=" + region +
                ", buyer=" + buyer +
                ", paymentType=" + paymentType +
                ", paymentMethod=" + paymentMethod +
                ", expectedStatus=" + expectedStatus +
                ", context=" + context +
                '}';
    }
}
