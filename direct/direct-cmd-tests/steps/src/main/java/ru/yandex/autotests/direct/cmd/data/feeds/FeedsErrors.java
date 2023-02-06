package ru.yandex.autotests.direct.cmd.data.feeds;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

/**
 * Created by aleran on 04.09.2015.
 */
public enum FeedsErrors implements ITextResource {

    FEEDS_DELETE_USED_ERROR,
    FEEDS_SAVE_COUNT_ERROR,
    FEEDS_SAVE_FILE_ABOVE_MAX_ERROR,
    FEED_NOT_FOUND,
    WRONG_BUSINESS_TYPE;

    private String value;

    @Override
    public String getBundle() {
        return "backend.feeds.FeedsErrors";
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
