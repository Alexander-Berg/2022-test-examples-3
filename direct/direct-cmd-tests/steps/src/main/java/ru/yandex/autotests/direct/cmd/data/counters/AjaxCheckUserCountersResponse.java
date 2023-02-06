package ru.yandex.autotests.direct.cmd.data.counters;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class AjaxCheckUserCountersResponse {

    @SerializedName("result")
    private HashMap<Long, MetrikaCounter> counterMap;

    public HashMap<Long, MetrikaCounter> getCounterMap() {
        return counterMap;
    }

    public void setCounterMap(HashMap<Long, MetrikaCounter> counterMap) {
        this.counterMap = counterMap;
    }
}
