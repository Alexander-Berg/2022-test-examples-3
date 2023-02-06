package ru.yandex.direct.internaltools.tools.testusers.container;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.direct.rbac.RbacRole;

@ParametersAreNonnullByDefault
public class TestUserInfo {

    @JsonProperty("Логин")
    private String login;

    @JsonProperty("Доменный логин")
    private String domainLogin;

    @JsonProperty("Роль")
    private RbacRole role;

    public String getLogin() {
        return login;
    }

    public String getDomainLogin() {
        return domainLogin;
    }

    public RbacRole getRole() {
        return role;
    }

    public TestUserInfo withLogin(String login) {
        this.login = login;
        return this;
    }

    public TestUserInfo withDomainLogin(String domainLogin) {
        this.domainLogin = domainLogin;
        return this;
    }

    public TestUserInfo withRole(RbacRole role) {
        this.role = role;
        return this;
    }
}
