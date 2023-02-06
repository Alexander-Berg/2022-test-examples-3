package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

public class WaveId {
    private String id;

    public WaveId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
