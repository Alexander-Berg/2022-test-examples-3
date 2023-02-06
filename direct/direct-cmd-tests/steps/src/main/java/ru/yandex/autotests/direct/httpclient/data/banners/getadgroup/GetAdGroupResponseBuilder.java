package ru.yandex.autotests.direct.httpclient.data.banners.getadgroup;

import java.util.List;

public class GetAdGroupResponseBuilder {
    private String pid;
    private String priorityID;
    private String geo;
    private String groupName;
    private String statusModerate;
    private String statusBsSynced;
    private String statusPostModerate;
    private String statusShowsForecast;
    private String statusAutobudgetShow;
    private Integer cid;
    private Integer mobileMultiplierPct;
    private String forecastDate;
    private String searchDesktopBanners;
    private String searchMobileBanners;
    private String contextPosterBanners;
    private String contextBanners;
    private String totalBanners;
    private List<String> canDeleteBanners;
    private List<String> canArchiveBanners;
    private List<String> bids;

    public GetAdGroupResponseBuilder setPid(String pid) {
        this.pid = pid;
        return this;
    }

    public GetAdGroupResponseBuilder setPriorityID(String priorityID) {
        this.priorityID = priorityID;
        return this;
    }

    public GetAdGroupResponseBuilder setGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public GetAdGroupResponseBuilder setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public GetAdGroupResponseBuilder setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public GetAdGroupResponseBuilder setStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public GetAdGroupResponseBuilder setStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
        return this;
    }

    public GetAdGroupResponseBuilder setStatusShowsForecast(String statusShowsForecast) {
        this.statusShowsForecast = statusShowsForecast;
        return this;
    }

    public GetAdGroupResponseBuilder setStatusAutobudgetShow(String statusAutobudgetShow) {
        this.statusAutobudgetShow = statusAutobudgetShow;
        return this;
    }

    public GetAdGroupResponseBuilder setCid(Integer cid) {
        this.cid = cid;
        return this;
    }

    public GetAdGroupResponseBuilder setMobileMultiplierPct(Integer mobileMultiplierPct) {
        this.mobileMultiplierPct = mobileMultiplierPct;
        return this;
    }

    public GetAdGroupResponseBuilder setForecastDate(String forecastDate) {
        this.forecastDate = forecastDate;
        return this;
    }

    public GetAdGroupResponseBuilder setSearchDesktopBanners(String searchDesktopBanners) {
        this.searchDesktopBanners = searchDesktopBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setSearchMobileBanners(String searchMobileBanners) {
        this.searchMobileBanners = searchMobileBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setContextPosterBanners(String contextPosterBanners) {
        this.contextPosterBanners = contextPosterBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setContextBanners(String contextBanners) {
        this.contextBanners = contextBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setTotalBanners(String totalBanners) {
        this.totalBanners = totalBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setCanDeleteBanners(List<String> canDeleteBanners) {
        this.canDeleteBanners = canDeleteBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setCanArchiveBanners(List<String> canArchiveBanners) {
        this.canArchiveBanners = canArchiveBanners;
        return this;
    }

    public GetAdGroupResponseBuilder setBids(List<String> bids) {
        this.bids = bids;
        return this;
    }

    public GetAdGroupResponse createGetAdGroupResponse() {
        return new GetAdGroupResponse(pid, priorityID, geo, groupName, statusModerate, statusBsSynced, statusPostModerate, statusShowsForecast, statusAutobudgetShow, cid, mobileMultiplierPct, forecastDate, searchDesktopBanners, searchMobileBanners, contextPosterBanners, contextBanners, totalBanners, canDeleteBanners, canArchiveBanners, bids);
    }
}