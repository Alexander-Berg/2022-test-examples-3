package ru.yandex.autotests.direct.httpclient.data.firsthelp;

public class CompleteOptimizingRequestBeanBuilder {
    private String cid;
    private String optimizationComment;
    private String optimizeRequestId;

    public CompleteOptimizingRequestBeanBuilder setCid(String cid) {
        this.cid = cid;
        return this;
    }

    public CompleteOptimizingRequestBeanBuilder setOptimizationComment(String optimizationComment) {
        this.optimizationComment = optimizationComment;
        return this;
    }

    public CompleteOptimizingRequestBeanBuilder setOptimizeRequestId(String optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
        return this;
    }

    public CompleteOptimizingRequestBean createCompleteOptimizingRequestBean() {
        return new CompleteOptimizingRequestBean(cid, optimizationComment, optimizeRequestId);
    }
}