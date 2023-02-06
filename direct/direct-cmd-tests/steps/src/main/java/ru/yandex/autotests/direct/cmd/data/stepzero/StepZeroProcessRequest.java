package ru.yandex.autotests.direct.cmd.data.stepzero;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class StepZeroProcessRequest extends BasicDirectRequest {

    @SerializeKey("for_agency")
    private String forAgency;

    @SerializeKey("type")
    private StepZeroProcessClientTypeEnum type;

    @SerializeKey("new_login")
    private String newLogin;

    @SerializeKey("login")
    private String login;

    public String getForAgency() {
        return forAgency;
    }

    public StepZeroProcessRequest withForAgency(String forAgency) {
        this.forAgency = forAgency;
        return this;
    }

    public StepZeroProcessClientTypeEnum getType() {
        return type;
    }

    public StepZeroProcessRequest withType(StepZeroProcessClientTypeEnum type) {
        this.type = type;
        return this;
    }

    public String getNewLogin() {
        return newLogin;
    }

    public StepZeroProcessRequest withNewLogin(String newLogin) {
        this.newLogin = newLogin;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public StepZeroProcessRequest withLogin(String login) {
        this.login = login;
        return this;
    }
}
