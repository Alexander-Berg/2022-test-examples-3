package ru.yandex.autotests.direct.cmd.data.mediaplan;

import java.util.Collections;
import java.util.List;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

public class SendOptimizeRequest extends BasicDirectRequest{
    public static SendOptimizeRequest create(Long cid, Long adGroupId, Long optimizeRequestId) {
        SendOptimizeRequest sendOptimizeRequest = new SendOptimizeRequest();
        sendOptimizeRequest.setAdgroupIds(Collections.singletonList(adGroupId));
        sendOptimizeRequest.setCid(cid);
        sendOptimizeRequest.setOptimizeRequestId(optimizeRequestId);
        return sendOptimizeRequest;
    }

    @SerializeKey("adgroup_ids")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> adgroupIds;

    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("optimize_request_id")
    private Long optimizeRequestId;

    public Long getOptimizeRequestId() {
        return optimizeRequestId;
    }

    public void setOptimizeRequestId(Long optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public List<Long>  getAdgroupIds() {
        return adgroupIds;
    }

    public void setAdgroupIds(List<Long> adgroupIds) {
        this.adgroupIds = adgroupIds;
    }
}
