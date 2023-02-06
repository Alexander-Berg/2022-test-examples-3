package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 19.06.15
 * TESTIRT-4998.
 */
public enum MediaBannersErrorTexts implements ITextResource {

    WRONG_HREF_FORMAT,
    NO_PHRASES,
    MINUS_WORDS_INTERSECT_KEYWORDS_ERROR,
    KEYWORDS_INTERSECT_MINUS_WORDS_ERROR;

    @Override
    public String getBundle() {
            return "http.mediabanners.errors";
    }

    @Override
    public String toString() {
            return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
