package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum PerformanceGroupsErrorTexts implements ITextResource {
    EMPTY_GROUP_NAME,
    BANNERS_NOT_FOUND,
    TOO_MANY_FILTERS,
    TOO_MANY_BANNERS,
    FILTERS_NOT_FOUND;

    @Override
    public String getBundle() {
        return "http.performancegroups.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
