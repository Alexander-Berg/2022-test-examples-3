package ru.yandex.autotests.direct.httpclient.data.firsthelp;

public class AcceptOptimizeRequestBeanBuilder {
    private String acceptType;
    private String archOld;
    private String cid;
    private String optimizeRequestId;
    private String phIds;
    private String stopOld;

    public AcceptOptimizeRequestBeanBuilder setAcceptType(String acceptType) {
        this.acceptType = acceptType;
        return this;
    }

    public AcceptOptimizeRequestBeanBuilder setArchOld(String archOld) {
        this.archOld = archOld;
        return this;
    }

    public AcceptOptimizeRequestBeanBuilder setCid(String cid) {
        this.cid = cid;
        return this;
    }

    public AcceptOptimizeRequestBeanBuilder setOptimizeRequestId(String optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
        return this;
    }

    public AcceptOptimizeRequestBeanBuilder setPhIds(String phIds) {
        this.phIds = phIds;
        return this;
    }

    public AcceptOptimizeRequestBeanBuilder setStopOld(String stopOld) {
        this.stopOld = stopOld;
        return this;
    }

    public AcceptOptimizeRequestBean createAcceptOptimizeRequestBean() {
        return new AcceptOptimizeRequestBean(acceptType, archOld, cid, optimizeRequestId, phIds, stopOld);
    }
}