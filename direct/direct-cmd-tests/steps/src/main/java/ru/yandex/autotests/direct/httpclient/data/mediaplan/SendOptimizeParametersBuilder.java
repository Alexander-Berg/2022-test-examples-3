package ru.yandex.autotests.direct.httpclient.data.mediaplan;

public class SendOptimizeParametersBuilder {
    private String adgroupIds;
    private String cid;
    private String optimizeRequestId;

    public SendOptimizeParametersBuilder setAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
        return this;
    }

    public SendOptimizeParametersBuilder setCid(String cid) {
        this.cid = cid;
        return this;
    }

    public SendOptimizeParametersBuilder setOptimizeRequestId(String optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
        return this;
    }

    public SendOptimizeParameters createSendOptimizeParameters() {
        return new SendOptimizeParameters(adgroupIds, cid, optimizeRequestId);
    }
}