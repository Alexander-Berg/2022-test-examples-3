package ru.yandex.market.clab.tms.service.ocr.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeLimit {
    private int percent;
    @JsonProperty("stopped_by_timeout")
    private boolean stoppedByTimeout;

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public boolean getStoppedByTimeout() {
        return stoppedByTimeout;
    }

    public void setStoppedByTimeout(boolean stoppedByTimeout) {
        this.stoppedByTimeout = stoppedByTimeout;
    }
}
