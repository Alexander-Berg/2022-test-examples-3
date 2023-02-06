package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class StopABTestRequest extends BasicDirectRequest {
    @SerializeKey("experiment_id")
    private Long experimentId;

    public Long getExperimentId() {
        return experimentId;
    }

    public StopABTestRequest withExperimentId(Long experimentId) {
        this.experimentId = experimentId;
        return this;
    }
}
