package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum BannerErrors implements ITextResource {

    ERROR_CANVAS_CREATIVE_OR_PERMISSION_DENIED;
    @Override
    public String getBundle() {
        return "cmd.banners.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
