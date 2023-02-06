package ru.yandex.autotests.direct.httpclient.data.firsthelp;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 07.05.15
 */
public class CompleteOptimizingRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "optimizationComment")
    private String optimizationComment;

    @JsonPath(requestPath = "optimize_request_id")
    private String optimizeRequestId;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getOptimizationComment() {
        return optimizationComment;
    }

    public void setOptimizationComment(String optimizationComment) {
        this.optimizationComment = optimizationComment;
    }

    public String getOptimizeRequestId() {
        return optimizeRequestId;
    }

    public void setOptimizeRequestId(String optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
    }

    public CompleteOptimizingRequestBean() {
    }

    public CompleteOptimizingRequestBean(String cid, String optimizationComment, String optimizeRequestId) {
        this.cid = cid;
        this.optimizationComment = optimizationComment;
        this.optimizeRequestId = optimizeRequestId;
    }
}
