package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum PerformanceFiltersErrors implements ITextResource {

    WRONG_FORMAT,
    GROUP_NOT_FOUND,
    FILTER_NOT_FOUND;

    private String value;

    @Override
    public String getBundle() {
        return "backend.performance.Filters";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
