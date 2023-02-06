package ru.yandex.autotests.direct.httpclient.data.textresources.clients;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.05.15
 */
public enum  ModifyUserErrors implements ITextResource {

    EMPTY_NAME,
    INCORRECT_PHONE,
    INCORRECT_EMAIL;

    @Override
    public String getBundle() {
        return "http.clients.ModifyUserErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
