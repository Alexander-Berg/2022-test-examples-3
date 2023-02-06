package ru.yandex.autotests.direct.cmd.data.excel.errors;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum ImageAdErrors implements ITextResource {
    MOBILE_IMAGE_AD_WARNING,
    NEED_IMAGE,
    WRONG_IMAGE,
    WRONG_URL,
    CANNOT_CHANGE_IMAGE_TYPE,
    CANNOT_CHANGE_BANNER_TYPE,
    MAX_BANNERS_COUNT_REACHED,
    NEED_HREF,
    WRONG_HREF,
    NEED_TEXT,
    NEED_MAIN_HREF,
    NEED_APP_HREF;

    @Override
    public String getBundle() {
        return "cmd.excel.errors.ImageAdErrors";
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
