package ru.yandex.autotests.direct.httpclient.data.CmdBeans.retargeting;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 04.02.15.
 */
public class RetargetingGroupCmdBean {

    @JsonPath(requestPath = "type", responsePath = "type")
    private String type;

    @JsonPath(requestPath = "goals", responsePath = "goals")
    private List<GoalCmdBean> goals;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<GoalCmdBean> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalCmdBean> goals) {
        this.goals = goals;
    }
}
