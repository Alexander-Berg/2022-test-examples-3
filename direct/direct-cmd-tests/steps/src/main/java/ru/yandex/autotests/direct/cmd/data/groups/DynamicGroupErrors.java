package ru.yandex.autotests.direct.cmd.data.groups;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum DynamicGroupErrors implements ITextResource {

    DOMAIN_GROUP_WITH_PERF_CONDITIONS,
    DUPLICATE_CONDITION,
    MAX_CONDITIONS_IN_GROUP,
    EMPTY_CONDITION_TITLE,
    SEARCH_PRICE_CAN_NOT_BE_LOWER_THAN,
    FEED_GROUP_WITH_DYNAMIC_CONDITIONS;

    @Override
    public String getBundle() {
        return "cmd.groups.DynamicErrors";
    }

    public String toString() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
