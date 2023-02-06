package ru.yandex.autotests.direct.cmd.data.firsthelp;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CompleteOptimizingRequest {
    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("optimizationComment")
    private String optimizationComment;

    @SerializeKey("optimize_request_id")
    private Long optimizeRequestId;

    public Long getCid() {
        return cid;
    }

    public CompleteOptimizingRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getOptimizationComment() {
        return optimizationComment;
    }

    public CompleteOptimizingRequest withOptimizationComment(String optimizationComment) {
        this.optimizationComment = optimizationComment;
        return this;
    }

    public Long getOptimizeRequestId() {
        return optimizeRequestId;
    }

    public CompleteOptimizingRequest withOptimizeRequestId(Long optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
        return this;
    }
}
