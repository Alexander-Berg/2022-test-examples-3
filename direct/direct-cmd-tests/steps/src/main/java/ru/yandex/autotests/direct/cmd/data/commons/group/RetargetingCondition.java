package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Именованное условие ретаргетинга, в БД представлено в таблице retargeting_conditions
 */
public class RetargetingCondition {

    public static final int MAX_CONDITION_ITEMS = 50;
    public final static String DEFAULT_GOAL_TIME = "30";

    public static RetargetingCondition getDefaultRetargetingCondition() {
        return new RetargetingCondition()
                .withConditionName("test")
                .withCondition(Collections.singletonList(new RetargetingConditionItem()
                        .withType(RetConditionItemType.OR.getValue())
                        .withGoals(Collections.singletonList(new RetargetingGoal()
                                .withGoalId(MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId().toString())
                                .withTime(DEFAULT_GOAL_TIME)))
                ));
    }

    public static RetargetingCondition fromApiRetargetingCondition(
            ru.yandex.autotests.directapi.common.api45.RetargetingCondition apiBean) {
        RetargetingCondition retargeting = new RetargetingCondition()
                .withRetCondId(apiBean.getRetargetingConditionID().longValue())
                .withConditionName(apiBean.getRetargetingConditionName())
                .withConditionDesc(apiBean.getRetargetingConditionDescription());
        List<RetargetingConditionItem> conditions = new ArrayList<>();
        for (ru.yandex.autotests.directapi.common.api45.RetargetingConditionItem beanCondition : apiBean.getRetargetingCondition()) {
            conditions.add(RetargetingConditionItem.fromApiRetargetingConditionItem(beanCondition));
        }
        retargeting.withCondition(conditions);
        return retargeting;
    }

    @SerializedName("ret_cond_id")
    private Long retCondId;

    @SerializedName("condition_name")
    private String conditionName;

    @SerializedName("condition_desc")
    private String conditionDesc;

    @SerializedName("is_accessible")
    private Integer isAccessible;

    @SerializedName("condition")
    private List<RetargetingConditionItem> condition;

    @SerializedName("campaigns")
    private List<Campaign> campaigns;

    public Long getRetCondId() {
        return retCondId;
    }

    public RetargetingCondition withRetCondId(Long retCondId) {
        this.retCondId = retCondId;
        return this;
    }

    public String getConditionName() {
        return conditionName;
    }

    public RetargetingCondition withConditionName(String conditionName) {
        this.conditionName = conditionName;
        return this;
    }

    public String getConditionDesc() {
        return conditionDesc;
    }

    public RetargetingCondition withConditionDesc(String conditionDesc) {
        this.conditionDesc = conditionDesc;
        return this;
    }

    public Integer getIsAccessible() {
        return isAccessible;
    }

    public RetargetingCondition withIsAccessible(Integer isAccessible) {
        this.isAccessible = isAccessible;
        return this;
    }

    public List<RetargetingConditionItem> getCondition() {
        return condition;
    }

    public RetargetingCondition withCondition(List<RetargetingConditionItem> conditions) {
        this.condition = conditions;
        return this;
    }

    public List<Campaign> getCampaigns() {
        return campaigns;
    }

    public RetargetingCondition withCampaigns(List<Campaign> campaigns) {
        this.campaigns = campaigns;
        return this;
    }
}
