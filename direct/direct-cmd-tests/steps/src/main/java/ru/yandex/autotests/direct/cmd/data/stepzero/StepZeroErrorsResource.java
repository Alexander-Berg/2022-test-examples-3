package ru.yandex.autotests.direct.cmd.data.stepzero;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum StepZeroErrorsResource implements ITextResource {
    NOT_YOUR_AGENCY,
    CANT_CREATE_CAMPAIGN_FOR_LITE_CLIENT;

    @Override
    public String getBundle() {
        return "http.stepzero.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
