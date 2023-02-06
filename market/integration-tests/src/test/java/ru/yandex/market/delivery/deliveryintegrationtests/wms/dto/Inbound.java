package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

public class Inbound {
    private long yandexId;
    private String fulfillmentId;
    private String partnerId;

    public Inbound(long yandexId, String fulfillmentId) {
        this(yandexId, fulfillmentId, fulfillmentId);
    }

    public Inbound(long yandexId, String fulfillmentId, String partnerId) {
        this.yandexId = yandexId;
        this.fulfillmentId = fulfillmentId;
        this.partnerId = partnerId;
    }

    public long getYandexId() {
        return yandexId;
    }

    public void setYandexId(long yandexId) {
        this.yandexId = yandexId;
    }

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public void setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

}
