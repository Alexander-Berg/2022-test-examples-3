package ru.yandex.autotests.direct.httpclient.data.mediaplan;


import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class SendOptimizeParameters  extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "adgroup_ids")
    private String adgroupIds;

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "optimize_request_id")
    private String optimizeRequestId;

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public void setAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
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

    public SendOptimizeParameters() {
    }

    public SendOptimizeParameters(String adgroupIds, String cid, String optimizeRequestId) {
        this.adgroupIds = adgroupIds;
        this.cid = cid;
        this.optimizeRequestId = optimizeRequestId;
    }
}
