package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

public class OptimizedCamp {
    @SerializedName("request_id")
    private Long requestId;

    public Long getRequestId() {
        return requestId;
    }

    public OptimizedCamp withRequestId(Long requestId) {
        this.requestId = requestId;
        return this;
    }
}
