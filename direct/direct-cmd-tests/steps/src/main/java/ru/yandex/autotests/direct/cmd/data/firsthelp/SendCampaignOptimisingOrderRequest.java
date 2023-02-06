package ru.yandex.autotests.direct.cmd.data.firsthelp;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SendCampaignOptimisingOrderRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("budget")
    private Long budget;

    @SerializeKey("improvement")
    private String improvement;

    @SerializeKey("agree")
    private String agree;

    @SerializeKey("by_support")
    private String bySupport;

    @SerializeKey("request_comment")
    private String requestComment;

    public String getImprovement() {
        return improvement;
    }

    public SendCampaignOptimisingOrderRequest withImprovement(String improvement) {
        this.improvement = improvement;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public SendCampaignOptimisingOrderRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public Long getBudget() {
        return budget;
    }

    public SendCampaignOptimisingOrderRequest withBudget(Long budget) {
        this.budget = budget;
        return this;
    }

    public String getAgree() {
        return agree;
    }

    public SendCampaignOptimisingOrderRequest withAgree(String agree) {
        this.agree = agree;
        return this;
    }

    public String getBySupport() {
        return bySupport;
    }

    public SendCampaignOptimisingOrderRequest withBySupport(String bySupport) {
        this.bySupport = bySupport;
        return this;
    }

    public String getRequestComment() {
        return requestComment;
    }

    public SendCampaignOptimisingOrderRequest withRequestComment(String requestComment) {
        this.requestComment = requestComment;
        return this;
    }
}
