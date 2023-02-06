package ru.yandex.autotests.direct.httpclient.data.textresources.phrases;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 29.05.15.
 */
public enum AjaxUpdateTestPhrases implements ITextResource {

    ONLY_STOP_WORDS,
    NORMALIZED_PHRASE,
    NOT_NORMALIZED_PHRASE,
    DOUBLE_MINUS_WORDS_PHRASE,
    REMOVED_DOUBLES_MINUS_WORDS_PHRASE;

    @Override
    public String getBundle() {
        return "http.phrases.ajaxupdate.testphrases";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
