package ru.yandex.autotests.direct.cmd.data.stat;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum StatErrors implements ITextResource {

    SHOW_REPORT_DATE_START_ERROR,
    SAVE_REPORT_NAME_ALREADY_EXIST_ERROR,
    SAVE_REPORT_NAME_IS_NECESSARY_ERROR;

    private String value;

    @Override
    public String getBundle() {
            return "cmd.stat.StatErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
