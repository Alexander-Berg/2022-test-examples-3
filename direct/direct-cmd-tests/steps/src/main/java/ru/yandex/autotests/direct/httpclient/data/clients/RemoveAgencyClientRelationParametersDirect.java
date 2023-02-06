package ru.yandex.autotests.direct.httpclient.data.clients;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class RemoveAgencyClientRelationParametersDirect extends BasicDirectFormParameters {
    public RemoveAgencyClientRelationParametersDirect() {
        super();
        ignoreEmptyParameters(true);
    }

    @FormParameter("client_login")
    private String clientLogin;
    @FormParameter("do_remove")
    private String doRemove;

    public String getDoRemove() {
        return doRemove;
    }

    public void setDoRemove(String doRemove) {
        this.doRemove = doRemove;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public void setClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
    }
}
