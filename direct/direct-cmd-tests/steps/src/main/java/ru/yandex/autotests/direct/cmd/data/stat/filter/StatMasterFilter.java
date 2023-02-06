package ru.yandex.autotests.direct.cmd.data.stat.filter;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.stat.filter.Data;

public class StatMasterFilter {

    @SerializedName("data")
    private Data data;

    @SerializedName("name")
    private String name;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
