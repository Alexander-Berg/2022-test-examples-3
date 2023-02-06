package ru.yandex.autotests.direct.httpclient.data.CmdBeans.stepzero;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 03.04.15.
 */
public class ClientsCmdBean {

    @JsonPath(responsePath = "clients/login")
    private List<String> logins;

    public List<String> getLogins() {
        return logins;
    }

    public void setLogins(List<String> logins) {
        this.logins = logins;
    }
}
