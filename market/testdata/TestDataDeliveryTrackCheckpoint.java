package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.BaseBean;

public class TestDataDeliveryTrackCheckpoint extends BaseBean {
    private long id;
    private Integer deliveryCheckpointStatus;
    private String checkpointDate;
    private String checkpointStatus;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Integer getDeliveryCheckpointStatus() {
        return deliveryCheckpointStatus;
    }

    public void setDeliveryCheckpointStatus(Integer deliveryCheckpointStatus) {
        this.deliveryCheckpointStatus = deliveryCheckpointStatus;
    }

    public String getCheckpointDate() {
        return checkpointDate;
    }

    public void setCheckpointDate(String checkpointDate) {
        this.checkpointDate = checkpointDate;
    }

    public void setCheckpointStatus(String checkpointStatus) {
        this.checkpointStatus = checkpointStatus;
    }

    public String getCheckpointStatus() {
        return checkpointStatus;
    }
}
