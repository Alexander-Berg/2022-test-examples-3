package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

/**
 *
 *     Статусы:
 *
 *     UNKNOWN(-1),
 *     NEW(1),
 *     PROCESSING(20),
 *     ACCEPTED(30),
 *     COMPLETED(40),
 *     ERROR(50);
 *
 */
public class Transfer {
    private long yandexId;
    private String fulfillmentId;

    public static int STATUS_UNKNOWN = -1;
    public static int STATUS_NEW = 1;
    public static int STATUS_PROCESSING = 20;
    public static int STATUS_ACCEPTED = 30;
    public static int STATUS_COMPLETED = 40;
    public static int STATUS_ERROR = 50;

    public Transfer(long yandexId, String fulfillmentId) {
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
}
