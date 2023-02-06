package ru.yandex.autotests.direct.cmd.data.stat.filter;

import com.google.gson.annotations.SerializedName;

public class Agoalincome {

    @SerializedName("gt")
    private String gt;

    public String getGt() {
        return gt;
    }

    public void setGt(String gt) {
        this.gt = gt;
    }
}
