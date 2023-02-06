package ru.yandex.autotests.direct.httpclient.data.clients;


import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class AddAgencyClientRelationParametersDirect extends AbstractFormParameters {
    public AddAgencyClientRelationParametersDirect() {
        super();
        ignoreEmptyParameters(true);
    }

    @FormParameter("client_login")
    private String clientLogin;
    @FormParameter("do_check")
    private String doCheck;
    @FormParameter("do_add")
    private String doAdd;

    public String getDoCheck() {
        return doCheck;
    }

    public void setDoCheck(String doCheck) {
        this.doCheck = doCheck;
    }

    public String getDoAdd() {
        return doAdd;
    }

    public void setDoAdd(String doAdd) {
        this.doAdd = doAdd;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public void setClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
    }
}
