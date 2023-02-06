package ru.yandex.autotests.direct.cmd.data.retargeting;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum DeleteRetConditionErrorsResource implements ITextResource {

    COND_ERR_USED,
    COND_ERR_NOT_FOUND;

    @Override
    public String getBundle() {
        return "cmd.retargetings.errors.DeleteRetargetingErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
