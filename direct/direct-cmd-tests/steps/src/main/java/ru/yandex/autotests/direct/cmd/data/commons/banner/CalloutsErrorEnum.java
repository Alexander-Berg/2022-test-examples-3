package ru.yandex.autotests.direct.cmd.data.commons.banner;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum CalloutsErrorEnum implements ITextResource {

    TOO_LONG_CALLOUT,
    WRONG_SYMBOLS,
    UNIQUE_CALLOUTS,
    TEXT_NOT_FOUND,
    MAX_CALLOUTS;

    @Override
    public String getBundle() {
        return "http.banners.callouts.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }

}
