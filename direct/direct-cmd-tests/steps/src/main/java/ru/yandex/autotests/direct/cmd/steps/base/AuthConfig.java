package ru.yandex.autotests.direct.cmd.steps.base;

import ru.yandex.autotests.direct.cmd.data.CSRFToken;

public class AuthConfig {
    
    private String login;
    private String password;
    private CSRFToken csrfToken;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CSRFToken getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(CSRFToken csrfToken) {
        this.csrfToken = csrfToken;
    }
}
