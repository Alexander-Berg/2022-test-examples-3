package ru.yandex.autotests.direct.httpclient.data.strategy;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * Created by alexey-n on 17.08.14.
 */
public class AjaxSaveAutoBudgetParameters extends AbstractFormParameters {

    @FormParameter("cid")
    private String cid;

    @FormParameter("ulogin")
    private String uLogin;

    @FormParameter("json_strategy")
    private String jsonStrategy;

    @FormParameter("json_day_budget")
    private String jsonDayBudget;

    public String getJsonStrategy() {
        return jsonStrategy;
    }

    public void setJsonStrategy(String jsonStrategy) {
        this.jsonStrategy = jsonStrategy;
    }

    public String getJsonDayBudget() {
        return jsonDayBudget;
    }

    public void setJsonDayBudget(String jsonDayBudget) {
        this.jsonDayBudget = jsonDayBudget;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getuLogin() {
        return uLogin;
    }

    public void setuLogin(String uLogin) {
        this.uLogin = uLogin;
    }
}
