package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.BaseBean;

import java.util.List;

/**
 * Трек как его пушит трекер
 */
public class TestDataDeliveryTrack extends BaseBean {
    private TestDataDeliveryTrackMeta deliveryTrackMeta;
    private List<TestDataDeliveryTrackCheckpoint> deliveryTrackCheckpoints;

    public TestDataDeliveryTrackMeta getDeliveryTrackMeta() {
        return deliveryTrackMeta;
    }

    public void setDeliveryTrackMeta(TestDataDeliveryTrackMeta deliveryTrackMeta) {
        this.deliveryTrackMeta = deliveryTrackMeta;
    }

    public List<TestDataDeliveryTrackCheckpoint> getDeliveryTrackCheckpoints() {
        return deliveryTrackCheckpoints;
    }

    public void setDeliveryTrackCheckpoints(List<TestDataDeliveryTrackCheckpoint> deliveryTrackCheckpoints) {
        this.deliveryTrackCheckpoints = deliveryTrackCheckpoints;
    }
}
