package ru.yandex.autotests.direct.httpclient.data.retargeting;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 01.10.14
 */

public class AjaxSaveRetargetingCondParameters extends AbstractFormParameters {

    @FormParameter("ulogin")
    private String uLogin;

    @FormParameter("json_retargeting_condition")
    private String jsonRetargetingCondition;

    public String getuLogin() {
        return uLogin;
    }

    public void setuLogin(String uLogin) {
        this.uLogin = uLogin;
    }

    public String getJsonRetargetingCondition() {
        return jsonRetargetingCondition;
    }

    public void setJsonRetargetingCondition(String jsonRetargetingCondition) {
        this.jsonRetargetingCondition = jsonRetargetingCondition;
    }
}
