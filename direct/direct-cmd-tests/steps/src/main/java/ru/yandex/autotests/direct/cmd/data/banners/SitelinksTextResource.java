package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.utils.textresource.ITextResource;

/*
* todo javadoc
*/
public enum SitelinksTextResource implements ITextResource {
    SITELINKS_TEXT_ONLY_FOR_TURKEY,
    SITELINKS_TEXT_ONLY_FOR_UKRAINE,
    INVALID_SYMBOLS,
    SITELINKS_TEXT_ONLY_FOR_KAZAKHSTAN;

    @Override
    public String getBundle() {
        return "cmd.banners.sitelinks.SitelinksTextResource";
    }
}
