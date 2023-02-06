package ru.yandex.autotests.direct.cmd.data.commons;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum CommonErrorsResource implements ITextResource {

    NO_RIGHTS_FOR_OPERATION,
    NO_RIGHTS_FOR_OPERATION_WITH_POINT,
    RIGHTS_CHECK_ERROR,
    PARAMETER_REQUIRED,
    OPERATION_NOT_PERMITTED_IN_SELECTED_INTERFACE,
    ONLY_DWB_ALLOWED,
    FIELD_MUST_NOT_BE_EMPTY,
    NOT_USUAL_INTERFACE_REQUEST,
    WRONG_DATA,
    WRONG_INPUT_DATA,
    INVALID_PARAMETER_FORMAT,
    INVALID_FIELD;

    @Override
    public String getBundle() {
        return "http.common.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
