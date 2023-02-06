package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;

public class AutoPrice {
    @SerializedName("auto")
    private Integer auto;

    public Integer getAuto() {
        return auto;
    }

    public void setAuto(Integer auto) {
        this.auto = auto;
    }
    
    public AutoPrice withAuto(Integer auto) {
        this.auto = auto;
        return this;
    }
}
