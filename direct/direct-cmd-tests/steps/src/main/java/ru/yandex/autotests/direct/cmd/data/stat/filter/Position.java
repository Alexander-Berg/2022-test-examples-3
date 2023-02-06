package ru.yandex.autotests.direct.cmd.data.stat.filter;

import com.google.gson.annotations.SerializedName;

public class Position {

    @SerializedName("eq")
    private String eq;

    public String getEq() {
        return eq;
    }

    public void setEq(String eq) {
        this.eq = eq;
    }
}
