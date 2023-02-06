package ru.yandex.autotests.direct.httpclient.data.firsthelp;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 07.05.15
 */
public class AcceptOptimizeRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "acceptType")
    private String acceptType;

    @JsonPath(requestPath = "archOld")
    private String archOld;

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "optimize_request_id")
    private String optimizeRequestId;

    @JsonPath(requestPath = "phids")
    private String phIds;

    @JsonPath(requestPath = "stopOld")
    private String stopOld;

    public String getAcceptType() {
        return acceptType;
    }

    public void setAcceptType(String acceptType) {
        this.acceptType = acceptType;
    }

    public String getArchOld() {
        return archOld;
    }

    public void setArchOld(String archOld) {
        this.archOld = archOld;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getOptimizeRequestId() {
        return optimizeRequestId;
    }

    public void setOptimizeRequestId(String optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
    }

    public String getPhIds() {
        return phIds;
    }

    public void setPhIds(String phIds) {
        this.phIds = phIds;
    }

    public String getStopOld() {
        return stopOld;
    }

    public void setStopOld(String stopOld) {
        this.stopOld = stopOld;
    }

    public AcceptOptimizeRequestBean() {
    }

    public AcceptOptimizeRequestBean(String acceptType, String archOld, String cid, String optimizeRequestId, String phIds, String stopOld) {
        this.acceptType = acceptType;
        this.archOld = archOld;
        this.cid = cid;
        this.optimizeRequestId = optimizeRequestId;
        this.phIds = phIds;
        this.stopOld = stopOld;
    }
}
