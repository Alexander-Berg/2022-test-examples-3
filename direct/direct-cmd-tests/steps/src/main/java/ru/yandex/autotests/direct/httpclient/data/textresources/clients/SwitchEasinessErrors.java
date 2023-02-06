package ru.yandex.autotests.direct.httpclient.data.textresources.clients;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 11.06.15
 */
public enum  SwitchEasinessErrors implements ITextResource {

    UNKNOWN_CLIENT_CURRENCY,
    PLEASE_SET_COUNTRY,
    SWITCH_INTERFACE_IMPOSSIBLE;

    @Override
    public String getBundle() {
        return "http.clients.SwitchEasinessErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
