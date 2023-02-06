package ru.yandex.autotests.direct.cmd.data.banners.additions;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum CalloutsErrorsTextResource implements ITextResource {
    CALLOUTS_MUST_BE_UNIQUE,
    REPEATABLE_ELEMENTS,
    MAX_LENGTH_EXCEEDED,
    INVALID_SYMBOLS,
    INVALID_SYMBOLS_SHORT,
    NUMBER_MORE_THAN_MAX,
    BANNER_CALLOUTS_MORE_THAN_MAX,
    ILLEGAL_CALLOUTS_NUMBER,
    EMPTY_TEXT,
    EMPTY_FIELD,
    ERROR_EMPTY_CALLOUT_ID,
    ERROR_INVALID_CALLOUT_ID,
    AD_EXTENSIONS_TEXT_ONLY_FOR_UKRAINE,
    AD_EXTENSIONS_TEXT_ONLY_FOR_KAZAKHSTAN,
    AD_EXTENSIONS_TEXT_ONLY_FOR_TURKEY;

    private String value;

    @Override
    public String getBundle() {
        return "cmd.banners.errors.CalloutsErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
