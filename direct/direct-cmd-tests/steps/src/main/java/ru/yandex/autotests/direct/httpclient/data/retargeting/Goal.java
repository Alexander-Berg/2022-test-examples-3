package ru.yandex.autotests.direct.httpclient.data.retargeting;


import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 01.10.14
 */

public class Goal  {

    @JsonProperty("goal_id")
    private String goalId;

    @JsonProperty("time")
    private String time;

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
