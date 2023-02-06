package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.Date;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;

public class CheckoutOptionParameters {

    private DeliveryType deliveryType;
    private DeliveryPartnerType deliveryPartnerType;
    private Long deliveryServiceId;
    private Long outletId;
    private Boolean freeDelivery;
    private Date fromDate;
    private Boolean leaveAtTheDoor;

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
    }

    public DeliveryPartnerType getDeliveryPartnerType() {
        return deliveryPartnerType;
    }

    public void setDeliveryPartnerType(DeliveryPartnerType deliveryPartnerType) {
        this.deliveryPartnerType = deliveryPartnerType;
    }

    public Long getDeliveryServiceId() {
        return deliveryServiceId;
    }

    public void setDeliveryServiceId(Long deliveryServiceId) {
        this.deliveryServiceId = deliveryServiceId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public void setOutletId(Long outletId) {
        this.outletId = outletId;
    }

    public Boolean isFreeDelivery() {
        return freeDelivery;
    }

    public void setFreeDelivery(Boolean freeDelivery) {
        this.freeDelivery = freeDelivery;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Boolean getLeaveAtTheDoor() {
        return leaveAtTheDoor;
    }

    public void setLeaveAtTheDoor(Boolean leaveAtTheDoor) {
        this.leaveAtTheDoor = leaveAtTheDoor;
    }
}
