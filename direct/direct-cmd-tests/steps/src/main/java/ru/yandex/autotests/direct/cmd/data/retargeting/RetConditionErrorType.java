package ru.yandex.autotests.direct.cmd.data.retargeting;

public enum RetConditionErrorType {

    RET_COND_EXISTS("exists_same_cond"),
    NEED_COND_NAME("need_cond_name"),
    INVALID_COND_TYPE("invalid_cond_type"),
    INVALID_GOAL_ID("wrong_goal_id"),
    INVALID_GOAL_TIME("wrong_goal_time"),
    ONLY_NOT("only_not_type_in_cond"),
    MAX_GOALS_EXCEEDED("max_retargeting_goals_in_group_exceeded"),
    INVALID_DATA("invalid_data"),
    ERROR("error"),
    MAX_GROUPS_EXCEEDED("max_retargeting_groups_exceeded");

    private String errType;

    RetConditionErrorType(String errType) {
        this.errType = errType;
    }

    @Override
    public String toString() {
        return errType;
    }
}
