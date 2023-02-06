package ru.yandex.autotests.direct.httpclient.data.retargeting;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * Created by shmykov on 26.01.15.
 * TESTIRT-4058
 */
public class AjaxDeleteRetargetingCondParameters extends AbstractFormParameters {

    @FormParameter("ulogin")
    private String uLogin;

    @FormParameter("ret_cond_id")
    private String retCondId;

    public String getuLogin() {
        return uLogin;
    }

    public void setuLogin(String uLogin) {
        this.uLogin = uLogin;
    }

    public String getRetCondId() {
        return retCondId;
    }

    public void setRetCondId(String retCondId) {
        this.retCondId = retCondId;
    }
}
