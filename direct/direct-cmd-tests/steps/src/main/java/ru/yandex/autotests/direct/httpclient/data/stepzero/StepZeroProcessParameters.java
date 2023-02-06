package ru.yandex.autotests.direct.httpclient.data.stepzero;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * Created by shmykov on 06.04.15.
 */
public class StepZeroProcessParameters extends AbstractFormParameters {

    @FormParameter("for_agency")
    private String forAgency;

    @FormParameter("type")
    private StepZeroProcessClientTypeEnum type;

    @FormParameter("new_login")
    private String newLogin;

    @FormParameter("login")
    private String login;

    public String getForAgency() {
        return forAgency;
    }

    public String getType() {
        return type.toSring();
    }

    public String getNewLogin() {
        return newLogin;
    }

    public String getLogin() {
        return login;
    }

    public StepZeroProcessParameters setForAgency(String forAgency) {
        this.forAgency = forAgency;
        return this;
    }

    public StepZeroProcessParameters setNewLogin(String newLogin) {
        this.newLogin = newLogin;
        return this;
    }

    public StepZeroProcessParameters setLogin(String login) {
        this.login = login;
        return this;
    }

    public StepZeroProcessParameters setType(StepZeroProcessClientTypeEnum type) {
        this.type = type;
        return this;
    }
}
