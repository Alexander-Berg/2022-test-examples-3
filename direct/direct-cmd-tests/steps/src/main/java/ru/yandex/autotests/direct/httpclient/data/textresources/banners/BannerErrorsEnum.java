package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum BannerErrorsEnum implements ITextResource {
    EMPTY_BANNER_TITLE,
    UNAVAILABLE_URL;

    @Override
    public String getBundle() {
        return "http.banners.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
