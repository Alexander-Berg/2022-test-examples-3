package ru.yandex.autotests.direct.httpclient.data.retargeting;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 29.09.14
 */
public class Condition {

    @JsonProperty("type")
    private String type;

    @JsonProperty("goals")
    private List<Goal> goals;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Goal> getGoals() {
        return goals;
    }

    public void setGoals(List<Goal> goals) {
        this.goals = goals;
    }
}
