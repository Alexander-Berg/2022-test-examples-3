package ru.yandex.direct.core.entity.testuser.model;

import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ru.yandex.direct.core.entity.testuser.serialize.TestUserRoleDeserializer;
import ru.yandex.direct.core.entity.testuser.serialize.TestUserRoleSerializer;
import ru.yandex.direct.rbac.RbacRole;

@ParametersAreNonnullByDefault
public class TestUser {

    @JsonIgnore
    private Long uid;

    private String login;

    @JsonProperty("domain_login")
    private String domainLogin;

    @JsonSerialize(using = TestUserRoleSerializer.class)
    @JsonDeserialize(using = TestUserRoleDeserializer.class)
    private RbacRole role;

    public TestUser(Long uid, String domainLogin, RbacRole role) {
        this.uid = uid;
        this.domainLogin = domainLogin;
        this.role = role;
    }

    public Long getUid() {
        return uid;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getDomainLogin() {
        return domainLogin;
    }

    public RbacRole getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestUser testUser = (TestUser) o;
        return Objects.equals(uid, testUser.uid) &&
                Objects.equals(domainLogin, testUser.domainLogin) &&
                role == testUser.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, domainLogin, role);
    }
}
