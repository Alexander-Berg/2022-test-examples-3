package ru.yandex.autotests.direct.cmd.data.strategy;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

public class AjaxSaveAutoBudgetRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("ulogin")
    private String uLogin;

    @SerializeKey("json_strategy")
    @SerializeBy(ValueToJsonSerializer.class)
    private CampaignStrategy jsonStrategy;

    @SerializeKey("json_day_budget")
    @SerializeBy(ValueToJsonSerializer.class)
    private DayBudget dayBudget;

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public AjaxSaveAutoBudgetRequest withDayBudget(
            DayBudget dayBudget)
    {
        this.dayBudget = dayBudget;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public AjaxSaveAutoBudgetRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getuLogin() {
        return uLogin;
    }

    public AjaxSaveAutoBudgetRequest withuLogin(String uLogin) {
        this.uLogin = uLogin;
        return this;
    }

    public CampaignStrategy getJsonStrategy() {
        return jsonStrategy;
    }

    public AjaxSaveAutoBudgetRequest withJsonStrategy(CampaignStrategy jsonStrategy) {
        this.jsonStrategy = jsonStrategy;
        return this;
    }
}

