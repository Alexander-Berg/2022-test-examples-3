package ru.yandex.autotests.direct.httpclient.data.textresources.phrases;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 29.05.15.
 */
public enum AjaxUpdateErrorsResourse implements ITextResource {

    PRICE_ABOVE_MAX,
    PRICE_BELOW_MIN,
    SHOULD_BE_ONE_ACTIVE_PHRASE,
    ONLY_STOP_WORDS,
    PRICE_ABOVE_MAX_NEW,
    PRICE_BELOW_MIN_NEW,
    ONLY_STOP_WORDS_NEW;

    @Override
    public String getBundle() {
        return "http.phrases.ajaxupdate.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
