package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 16.10.14.
 */
public enum MinusWordsEnum implements ITextResource {

    NORMAL_MINUS_WORDS,
    NORMAL_KEY_WORDS,
    EXCLAMATION_MARK_MINUS_WORDS,
    PLUS_MARK_MINUS_WORDS,
    WRONG_CHARS_MINUS_WORDS,
    TOO_LONG_MINUS_WORDS,
    MINUS_WORDS_COMMA_SEPARATED,
    LOTS_OF_MINUS_WORDS,
    COMMON_WORD_FORM_MINUS_WORDS,
    COGNATE_MINUS_WORDS;

    @Override
    public String getBundle() {
        return "http.banners.minusWords";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}

