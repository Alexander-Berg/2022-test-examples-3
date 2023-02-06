package ru.yandex.autotests.direct.cmd.data.groups;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum DemographyMultiplierGroupErrors implements ITextResource {
    ERROR_CONDITIONS_COUNT_TEXT,
    ERROR_INTERSECT_CONDITIONS_TEXT,
    ERROR_INCORRECT_DATA,
    ERROR_NULL_DATA;


    private String value;

    @Override
    public String getBundle() {
        return "backend.groups.DemographyMultiplierErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
