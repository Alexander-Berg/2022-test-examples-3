package ru.yandex.autotests.direct.httpclient.data.CmdBeans.retargeting;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 04.02.15.
 */
public class GoalCmdBean {

    @JsonPath(requestPath = "id", responsePath = "id")
    private String goalID;
    private String time;

    public String getGoalID() {
        return goalID;
    }

    public void setGoalID(String id) {
        this.goalID = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}