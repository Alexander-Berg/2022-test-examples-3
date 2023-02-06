package ru.yandex.autotests.direct.cmd.data.showcamp;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/**
 * Created by ssdmitriev on 10.11.15.
 */
public class ShowCampRequest {
    public static ShowCampRequest createDefaultRequestForOneGroup(String client, Long groupId, Long campaignId) {
        return new ShowCampRequest()
                .withGroupId(groupId)
                .withmReverse(1)
                .withmSort("addtime")
                .withWp(1)
                .withWsCid(0)
                .withWsClient(client)
                .withWsDone(0)
                .withWsTime(1)
                .withPage("all")
                .withWsPlace("0")
                .withCid(String.valueOf(campaignId))
                .withUlogin(client);
    }
    @SerializeKey("cid")
    private String cid;
    @SerializeKey("ulogin")
    private String ulogin;
    @SerializeKey("search_banner")
    private String searchBanner;
    @SerializeKey("search_by")
    private String searchBy;
    @SerializeKey("tab")
    private String tab;
    @SerializeKey("interface")
    private String interfaceType;
    @SerializeKey("optimizeCamp")
    private String optimizeCamp;
    @SerializeKey("page")
    private String page;
    @SerializeKey("ws_place")
    private String wsPlace;
    @SerializeKey("sb")
    private String sb;
    @SerializeKey("uid_url")
    private String uidUrl;

    @SerializeKey("adgroup_id")
    private Long groupId;

    @SerializeKey("ws_time")
    private Integer wsTime;

    @SerializeKey("ws_cid")
    private Integer wsCid;

    @SerializeKey("mreverse")
    private Integer mReverse;

    @SerializeKey("ws_client")
    private String wsClient;

    @SerializeKey("wp")
    private Integer wp;


    @SerializeKey("msort")
    private String mSort;

    @SerializeKey("ws_done")
    private Integer wsDone;

    @SerializeKey("disabled_geo_only")
    private Integer disabledGeoOnly;

    public String getmSort() {
        return mSort;
    }

    public ShowCampRequest withmSort(String mSort) {
        this.mSort = mSort;
        return this;
    }

    public Integer getWsDone() {
        return wsDone;
    }

    public ShowCampRequest withWsDone(Integer wsDone) {
        this.wsDone = wsDone;
        return this;
    }

    public Integer getWp() {
        return wp;
    }

    public ShowCampRequest withWp(Integer wp) {
        this.wp = wp;
        return this;
    }

    public String getWsClient() {
        return wsClient;
    }

    public ShowCampRequest withWsClient(String wsClient) {
        this.wsClient = wsClient;
        return this;
    }

    public Integer getmReverse() {
        return mReverse;
    }

    public ShowCampRequest withmReverse(Integer mReverse) {
        this.mReverse = mReverse;
        return this;
    }

    public Integer getWsCid() {
        return wsCid;
    }

    public ShowCampRequest withWsCid(Integer wsCid) {
        this.wsCid = wsCid;
        return this;
    }

    public Integer getWsTime() {
        return wsTime;
    }

    public ShowCampRequest withWsTime(Integer wsTime) {
        this.wsTime = wsTime;
        return this;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public ShowCampRequest withGroupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }


    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getUlogin() {
        return ulogin;
    }

    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
    }

    public ShowCampRequest withUlogin(String ulogin) {
        this.ulogin = ulogin;
        return this;
    }

    public ShowCampRequest withCid(String cid) {
        this.cid= cid;
        return this;
    }
    public String getSearchBanner() {
        return searchBanner;
    }

    public void setSearchBanner(String searchBanner) {
        this.searchBanner = searchBanner;
    }

    public String getSearchBy() {
        return searchBy;
    }

    public void setSearchBy(String searchBy) {
        this.searchBy = searchBy;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getOptimizeCamp() {
        return optimizeCamp;
    }

    public void setOptimizeCamp(String optimizeCamp) {
        this.optimizeCamp = optimizeCamp;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public ShowCampRequest withPage(String page) {
        this.page = page;
        return this;
    }
    public String getWsPlace() {
        return wsPlace;
    }

    public void setWsPlace(String wsPlace) {
        this.wsPlace = wsPlace;
    }

    public ShowCampRequest withWsPlace(String wsPlace) {
        this.wsPlace = wsPlace;
        return this;
    }

    public String getSb() {
        return sb;
    }

    public void setSb(String sb) {
        this.sb = sb;
    }

    public String getUidUrl() {
        return uidUrl;
    }

    public void setUidUrl(String uidUrl) {
        this.uidUrl = uidUrl;
    }

    public ShowCampRequest withTab(String tab) {
        this.tab = tab;
        return this;
    }

    public Integer getDisabledGeoOnly() {
        return disabledGeoOnly;
    }

    public ShowCampRequest withDisabledGeoOnly(Integer disabledGeoOnly) {
        this.disabledGeoOnly = disabledGeoOnly;
        return this;
    }
}
