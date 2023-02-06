package ru.yandex.autotests.direct.cmd.steps.auth;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class PassportAuthBean {

    @SerializeKey
    private String login;

    @SerializeKey
    private String passwd;

    public PassportAuthBean(String login, String password) {
        this.login = login;
        this.passwd = password;
    }
}
