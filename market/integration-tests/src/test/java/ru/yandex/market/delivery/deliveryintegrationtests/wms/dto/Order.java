package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

public class Order {
    private long yandexId;
    private String fulfillmentId;

    public Order(long yandexId, String fulfillmentId) {
        this.yandexId = yandexId;
        this.fulfillmentId = fulfillmentId;
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

    @Override
    public String toString() {
        return "Order{" +
                "yandexId=" + yandexId +
                ", fulfillmentId='" + fulfillmentId + '\'' +
                '}';
    }
}
