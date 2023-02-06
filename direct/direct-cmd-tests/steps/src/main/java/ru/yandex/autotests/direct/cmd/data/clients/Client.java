package ru.yandex.autotests.direct.cmd.data.clients;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class Client {

    @SerializeKey("can_use_day_budget")
    @SerializedName("can_use_day_budget")
    private Integer canUseDayBudget;

    public Integer getCanUseDayBudget() {
        return canUseDayBudget;
    }

    public void setCanUseDayBudget(Integer canUseDayBudget) {
        this.canUseDayBudget = canUseDayBudget;
    }
}
