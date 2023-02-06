package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

public class DataAB {
    @SerializedName("experiment_id")
    private Long experimentId;

    public Long getExperimentId() {
        return experimentId;
    }

    public DataAB withExperimentId(Long experimentId) {
        this.experimentId = experimentId;
        return this;
    }
}
