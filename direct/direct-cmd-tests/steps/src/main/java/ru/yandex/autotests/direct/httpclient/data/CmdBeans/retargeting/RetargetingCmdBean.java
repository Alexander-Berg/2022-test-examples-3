package ru.yandex.autotests.direct.httpclient.data.CmdBeans.retargeting;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 04.02.15.
 * TESTIRT-4179
 * Бин, отражающий структуру retargetings в ответе контроллеров showCampMultiEdit, showCampMultiEditLight,
 * и в запросе к этим контроллерам
 */
public class RetargetingCmdBean {

    @JsonPath(requestPath = "ret_cond_id", responsePath = "ret_cond_id")
    private String retargetingConditionID;

    @JsonPath(requestPath = "condition_name", responsePath = "condition_name")
    private String retargetingConditionName;

    @JsonPath(requestPath = "condition_desc", responsePath = "condition_desc")
    private String retargetingConditionDescription;

    @JsonPath(requestPath = "is_accessible", responsePath = "is_accessible")
    private String isAccessible;

    @JsonPath(requestPath = "price_context", responsePath = "price_context")
    private String priceContext;

    @JsonPath(requestPath = "groups", responsePath = "groups")
    private List<RetargetingGroupCmdBean> retargetingCondition;

    private String clicks;

    private String shows;

    private String ctr;

    public String getRetargetingConditionID() {
        return retargetingConditionID;
    }

    public void setRetargetingConditionID(String retargetingConditionID) {
        this.retargetingConditionID = retargetingConditionID;
    }

    public String getRetargetingConditionName() {
        return retargetingConditionName;
    }

    public void setRetargetingConditionName(String retargetingConditionName) {
        this.retargetingConditionName = retargetingConditionName;
    }

    public String getRetargetingConditionDescription() {
        return retargetingConditionDescription;
    }

    public void setRetargetingConditionDescription(String retargetingConditionDescription) {
        this.retargetingConditionDescription = retargetingConditionDescription;
    }

    public String getClicks() {
        return clicks;
    }

    public void setClicks(String clicks) {
        this.clicks = clicks;
    }

    public String getShows() {
        return shows;
    }

    public void setShows(String shows) {
        this.shows = shows;
    }

    public String getCtr() {
        return ctr;
    }

    public void setCtr(String ctr) {
        this.ctr = ctr;
    }

    public List<RetargetingGroupCmdBean> getRetargetingCondition() {
        return retargetingCondition;
    }

    public void setRetargetingCondition(List<RetargetingGroupCmdBean> retargetingCondition) {
        this.retargetingCondition = retargetingCondition;
    }

    public String getIsAccessible() {
        return isAccessible;
    }

    public void setIsAccessible(String isAccessible) {
        this.isAccessible = isAccessible;
    }

    public String getPriceContext() {
        return priceContext;
    }

    public void setPriceContext(String priceContext) {
        this.priceContext = priceContext;
    }
}
