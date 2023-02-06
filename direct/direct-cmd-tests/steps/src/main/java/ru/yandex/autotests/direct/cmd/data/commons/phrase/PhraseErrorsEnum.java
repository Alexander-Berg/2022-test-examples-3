package ru.yandex.autotests.direct.cmd.data.commons.phrase;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum PhraseErrorsEnum implements ITextResource {

    TOO_LONG_PHRASE,
    TOO_LONG_WORD,
    TOO_MANY_PHRASES,
    TOO_MANY_WORDS_IN_PHRASE,
    TOO_MANY_WORDS_IN_PHRASE_NEW,
    WRONG_DOT_POSITION,
    WRONG_SYMBOLS,
    WRONG_START_SYMBOL,
    WRONG_QUOTE_USE,
    PHRASE_TEXT_NOT_FOUND,
    WRONG_ONLY_MINUS_WORDS,
    THE_MAXIMUM_AMOUNT_OF_KEYWORDS_EXCEED,
    KEY_WORDS_ARE_NOT_EDITABLE,
    KEY_WORDS_ARE_NOT_EDITABLE_GROUP_ARCHIVED,
    KEY_WORDS_ARE_NOT_EDITABLE_GROUP_ARCHIVED_NEW;

    @Override
    public String getBundle() {
        return "http.groups.phrases.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }

}
