package ru.yandex.autotests.direct.cmd.data.retargeting;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum RetConditionErrorsResource implements ITextResource {

    RET_COND_EXISTS,
    RET_COND_WITH_NAME_EXISTS,
    NEED_COND_NAME,
    INVALID_COND_TYPE,
    INVALID_GOAL_ID,
    INVALID_GOAL_TIME,
    INVALID_USERS_DATA,
    MAX_GOALS_EXCEEDED,
    MAX_GROUPS_EXCEEDED,
    AUDIENCE_UNAVAILABLE,
    ADD_ONE_GOAL,
    UNAVAILABLE_COND_CHANGE;
    @Override
    public String getBundle() {
        return "cmd.retargetings.errors.RetConditionErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
