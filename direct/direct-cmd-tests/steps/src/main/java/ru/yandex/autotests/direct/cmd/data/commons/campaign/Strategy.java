package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Strategy {

    @SerializedName("is_autobudget")
    private String isAutobudget;

    @SerializedName("is_search_stop")
    private String isSearchStop;

    @SerializedName("name")
    private String name;

    @SerializedName("is_net_stop")
    private String isNetStop;

    public String getIsAutobudget() {
        return isAutobudget;
    }

    public Strategy withIsAutobudget(String isAutobudget) {
        this.isAutobudget = isAutobudget;
        return this;
    }

    public String getIsSearchStop() {
        return isSearchStop;
    }

    public Strategy withIsSearchStop(String isSearchStop) {
        this.isSearchStop = isSearchStop;
        return this;
    }

    public String getName() {
        return name;
    }

    public Strategy withName(String name) {
        this.name = name;
        return this;
    }

    public String getIsNetStop() {
        return isNetStop;
    }

    public Strategy withIsNetStop(String isNetStop) {
        this.isNetStop = isNetStop;
        return this;
    }
}
