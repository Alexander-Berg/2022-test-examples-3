package ru.yandex.autotests.direct.cmd.data.stat.filter;

import com.google.gson.annotations.SerializedName;

public class JsonFiltersSet {

    @SerializedName("name")
    private String name;

    @SerializedName("data")
    private Data data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
