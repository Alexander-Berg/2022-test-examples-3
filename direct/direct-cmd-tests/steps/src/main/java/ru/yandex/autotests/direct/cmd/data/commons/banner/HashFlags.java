package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class HashFlags {

    @SerializedName("forex")
    public Integer forex;

    @SerializedName("age")
    private String age;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public Integer getForex() {
        return forex;
    }

    public void setForex(Integer forex) {
        this.forex = forex;
    }

    public HashFlags withAge(String age) {
        this.age = age;
        return this;
    }
}
