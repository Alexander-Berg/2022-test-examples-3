package ru.yandex.autotests.direct.httpclient.data.textresources.retargetings;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 26.01.15.
 */
public enum RetargetingsErrorsResource implements ITextResource {

    NO_CONDITION_FOUND,
    CONDITION_IS_USED,
    INTEREST_CATEGORY_MUST_BE_NOT_UNIQUE,
    AT_LEAST_ONE_CONDITION_MUST_BE,
    ;

    @Override
    public String getBundle() {
        return "http.retargetings.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
