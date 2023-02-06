package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum GroupsErrorsEnum implements ITextResource {
    TOO_MANY_BANNERS,
    RETARGETING_CONDITION_NOT_FOUND,
    BANNERS_NOT_FOUND,
    EMPTY_GROUP_NAME,
    EMPTY_ADDRESS,
    EMPTY_PHRASES;

    @Override
    public String getBundle() {
        return "http.groups.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
