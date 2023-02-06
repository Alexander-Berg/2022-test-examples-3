package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Строки для проверки значения banner.status в ответе showCamp/getAdGroup
 */
public enum BannerStatusTexts implements ITextResource {
    VIDEO_ADDITION_DECLINED,
    VIDEO_ADDITION_WAIT_MODERATION;

    @Override
    public String getBundle() {
        return "cmd.banner.statuses";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
