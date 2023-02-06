package ru.yandex.autotests.direct.httpclient.data.firsthelp;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class SendCampaignOptimizingOrderParameters extends BasicDirectRequestParameters {
    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "budget")
    private String budget;
    @JsonPath(requestPath = "improvement")
    private String improvementText;
    @JsonPath(requestPath = "improvement")
    private String improvementKeyword;
    @JsonPath(requestPath = "improvement")
    private String improvementMediaplaner;
    @JsonPath(requestPath = "agree")
    private String agree;
    @JsonPath(requestPath = "by_support")
    private String bySupport;
    @JsonPath(requestPath = "request_comment")
    private String requestComment;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getImprovementText() {
        return improvementText;
    }

    public void setImprovementText(String improvementText) {
        this.improvementText = improvementText;
    }

    public String getImprovementKeyword() {
        return improvementKeyword;
    }

    public void setImprovementKeyword(String improvementKeyword) {
        this.improvementKeyword = improvementKeyword;
    }

    public String getImprovementMediaplaner() {
        return improvementMediaplaner;
    }

    public void setImprovementMediaplaner(String improvementMediaplaner) {
        this.improvementMediaplaner = improvementMediaplaner;
    }

    public String getAgree() {
        return agree;
    }

    public void setAgree(String agree) {
        this.agree = agree;
    }

    public String getBySupport() {
        return bySupport;
    }

    public void setBySupport(String bySupport) {
        this.bySupport = bySupport;
    }

    public String getRequestComment() {
        return requestComment;
    }

    public void setRequestComment(String requestComment) {
        this.requestComment = requestComment;
    }
}
