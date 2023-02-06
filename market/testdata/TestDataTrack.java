package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.BaseBean;
import ru.yandex.autotests.market.checkouter.beans.TrackStatus;

import java.util.List;

/**
 * Трэк в АПИ чекаутера
 */
public class TestDataTrack extends BaseBean {
    private long deliveryServiceId;
    private String trackCode;
    // ид. трека в трекере, нужно для нотификаций
    private long trackerId;
    private List<TestDataTrackCheckpoint> checkpoints;
    private TrackStatus status;

    public long getDeliveryServiceId() {
        return deliveryServiceId;
    }

    public void setDeliveryServiceId(long deliveryServiceId) {
        this.deliveryServiceId = deliveryServiceId;
    }

    public String getTrackCode() {
        return trackCode;
    }

    public void setTrackCode(String trackCode) {
        this.trackCode = trackCode;
    }

    public long getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(long trackerId) {
        this.trackerId = trackerId;
    }

    public List<TestDataTrackCheckpoint> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<TestDataTrackCheckpoint> checkpoints) {
        this.checkpoints = checkpoints;
    }

    public TrackStatus getStatus() {
        return status;
    }

    public void setStatus(TrackStatus status) {
        this.status = status;
    }
}
