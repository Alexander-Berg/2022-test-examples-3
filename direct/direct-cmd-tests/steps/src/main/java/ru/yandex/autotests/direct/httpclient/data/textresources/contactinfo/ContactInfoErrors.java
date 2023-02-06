package ru.yandex.autotests.direct.httpclient.data.textresources.contactinfo;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 18.12.14.
 */
public enum ContactInfoErrors implements ITextResource {

    NO_COUNTRY,
    NO_CITY,
    NO_PHONE,
    WRONG_PHONE,
    WRONG_EXT_PHONE,
    NO_NAME,
    NO_WORKTIME,
    WRONG_WORKTIME,
    TOO_LONG_EXTRA_MESSAGE,
    WRONG_EMAIL,
    WRONG_IM_CLIENT,
    WRONG_ICQ_LOGIN,
    WRONG_JABBER,
    WRONG_OGRN;

    @Override
    public String getBundle() {
        return "http.groups.contactInfoErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
