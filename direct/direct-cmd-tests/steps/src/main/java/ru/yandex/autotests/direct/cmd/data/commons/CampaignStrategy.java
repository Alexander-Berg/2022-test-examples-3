package ru.yandex.autotests.direct.cmd.data.commons;


import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class CampaignStrategy {

    @JsonPath(responsePath = "name")
    private String name;
    @SerializedName("is_net_stop")
    @JsonPath(responsePath = "is_net_stop")
    private String isNetStop = "0";
    @SerializedName("is_search_stop")
    @JsonPath(responsePath = "is_search_stop")
    private String isSearchStop = "0";
    @JsonPath(responsePath = "search")
    private Strategy search;
    @JsonPath(responsePath = "net")
    private Strategy net;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Strategy getSearch() {
        return search;
    }

    public void setSearch(Strategy search) {
        this.search = search;
    }

    public Strategy getNet() {
        return net;
    }

    public void setNet(Strategy net) {
        this.net = net;
    }

    public String getIsNetStop() {
        return isNetStop;
    }

    public void setIsNetStop(String isNetStop) {
        this.isNetStop = isNetStop;
    }

    public String getIsSearchStop() {
        return isSearchStop;
    }

    public void setIsSearchStop(String isSearchStop) {
        this.isSearchStop = isSearchStop;
    }

    public CampaignStrategy withName(String name) {
        this.name = name;
        return this;
    }

    public CampaignStrategy withSearch(Strategy search) {
        this.search = search;
        return this;
    }

    public CampaignStrategy withNet(Strategy net) {
        this.net = net;
        return this;
    }

    public CampaignStrategy withIsNetStop(String isNetStop) {
        this.isNetStop = isNetStop;
        return this;
    }

    public CampaignStrategy withIsSearchStop(String isSearchStop) {
        this.isSearchStop = isSearchStop;
        return this;
    }

    public String toJson() {
        return new Gson().toJson(this, this.getClass());
    }

    public String toString() {
        return toJson();
    }

}
