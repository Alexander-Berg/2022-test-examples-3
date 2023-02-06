package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 16.10.14.
 */
public enum MinusWordsErrorTexts implements ITextResource {

    TOO_LONG_MINUS_WORDS_ERROR,
    TOO_LONG_ONE_MINUS_WORD_ERROR,
    WRONG_CHARS_IN_MINUS_WORDS_ERROR,
    WRONG_CHARS_IN_MINUS_PHRASE_ERROR2,
    MINUS_WORDS_INTERSECT_KEYWORDS_ERROR2,
    KEYWORDS_INTERSECT_MINUS_WORDS_ERROR2;

    @Override
    public String getBundle() {
            return "http.banners.minusWordsErrors";
    }

    @Override
    public String toString() {
            return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
