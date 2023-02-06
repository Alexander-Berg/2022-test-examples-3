package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ShowAbTestResponse {
    @SerializedName("experiments")
    Map<Long, Experiment> experimentMap;

    public Map<Long, Experiment> getExperimentMap() {
        return experimentMap;
    }

    public ShowAbTestResponse withExperimentMap(Map<Long, Experiment> experimentMap) {
        this.experimentMap = experimentMap;
        return this;
    }
}
