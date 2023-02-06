package ru.yandex.direct.internaltools.tools.testusers.model;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Text;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.tools.testusers.preprocessors.TestUserRbacRolePreProcessor;
import ru.yandex.direct.rbac.RbacRole;

@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class TestUsersParameters extends InternalToolParameter {

    public static final String LOGIN = "Логин";
    public static final String DOMAIN_LOGIN = "Доменный логин";
    public static final String RETRACT_ROLE = "Прекратить выдавать роль";
    public static final String ROLE = "Роль";

    @Input(label = LOGIN)
    @Text(valueMaxLen = 30)
    private String login;

    @Input(label = DOMAIN_LOGIN, required = false)
    @Text(valueMaxLen = 30)
    private String domainLogin;

    @Input(label = RETRACT_ROLE)
    private boolean retractRole;

    @Input(label = ROLE, processors = {TestUserRbacRolePreProcessor.class})
    private RbacRole role;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getDomainLogin() {
        return domainLogin;
    }

    public void setDomainLogin(String domainLogin) {
        this.domainLogin = domainLogin;
    }

    public boolean isRetractRole() {
        return retractRole;
    }

    public void setRetractRole(boolean retractRole) {
        this.retractRole = retractRole;
    }

    public RbacRole getRole() {
        return role;
    }

    public void setRole(RbacRole role) {
        this.role = role;
    }
}
