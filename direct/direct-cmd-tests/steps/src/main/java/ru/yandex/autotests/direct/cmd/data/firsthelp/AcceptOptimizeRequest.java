package ru.yandex.autotests.direct.cmd.data.firsthelp;

import java.util.List;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AcceptOptimizeRequest extends BasicDirectRequest {
    @SerializeKey("acceptType")
    private String acceptType;

    @SerializeKey("archOld")
    private String archOld;

    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("optimize_request_id")
    private Long optimizeRequestId;

    @SerializeKey("phids")
    private List<Long>  phIds;

    @SerializeKey("stopOld")
    private String stopOld;

    public String getAcceptType() {
        return acceptType;
    }

    public AcceptOptimizeRequest withAcceptType(String acceptType) {
        this.acceptType = acceptType;
        return this;
    }

    public String getArchOld() {
        return archOld;
    }

    public AcceptOptimizeRequest withArchOld(String archOld) {
        this.archOld = archOld;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public AcceptOptimizeRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public Long getOptimizeRequestId() {
        return optimizeRequestId;
    }

    public AcceptOptimizeRequest withOptimizeRequestId(Long optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
        return this;
    }

    public List<Long>  getPhIds() {
        return phIds;
    }

    public AcceptOptimizeRequest withPhIds(List<Long> phIds) {
        this.phIds = phIds;
        return this;
    }

    public String getStopOld() {
        return stopOld;
    }

    public AcceptOptimizeRequest withStopOld(String stopOld) {
        this.stopOld = stopOld;
        return this;
    }
}
