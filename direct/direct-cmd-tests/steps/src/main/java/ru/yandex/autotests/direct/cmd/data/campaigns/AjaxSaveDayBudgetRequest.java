package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

public class AjaxSaveDayBudgetRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private String cid;
    
    @SerializeKey("json_day_budget")
    @SerializeBy(ValueToJsonSerializer.class)
    private DayBudget dayBudget;

    public String getCid() {
        return cid;
    }

    public AjaxSaveDayBudgetRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public AjaxSaveDayBudgetRequest withDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }
}
