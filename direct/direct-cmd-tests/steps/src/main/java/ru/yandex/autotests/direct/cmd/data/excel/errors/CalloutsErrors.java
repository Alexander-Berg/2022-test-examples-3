package ru.yandex.autotests.direct.cmd.data.excel.errors;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

/**
 * Created by sudar on 02.02.2016.
 */
public enum CalloutsErrors implements ITextResource {
    MAX_CALLOUT_LENGTH_EXCEEDED_ERROR,
    CALLOUTS_ARE_THE_SAME_ERROR,
    WRONG_SYMBOLS_ERROR,
    DIFFERENT_CALLOUTS_IN_ROWS_ERROR;

    private String value;

    @Override
    public String getBundle() {
        return "cmd.excel.errors.CalloutsErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
