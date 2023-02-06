package ru.yandex.autotests.direct.httpclient.data.banners.getadgroup;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.03.15
 *         структура группы похожа на ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.GroupFakeInfo
 */
public class GetAdGroupResponse {

    @JsonPath(responsePath = "pid")
    private String pid;
    @JsonPath(responsePath = "PriorityID")
    private String priorityID;
    @JsonPath(responsePath = "geo")
    private String geo;
    @JsonPath(responsePath = "group_name")
    private String groupName;
    @JsonPath(responsePath = "statusModerate")
    private String statusModerate;
    @JsonPath(responsePath = "statusBsSynced")
    private String statusBsSynced;
    @JsonPath(responsePath = "statusPostModerate")
    private String statusPostModerate;
    @JsonPath(responsePath = "statusShowsForecast")
    private String statusShowsForecast;
    @JsonPath(responsePath = "statusAutobudgetShow")
    private String statusAutobudgetShow;
    @JsonPath(responsePath = "cid")
    private Integer cid;
    @JsonPath(responsePath = "mobile_multiplier_pct")
    private Integer mobileMultiplierPct;
    @JsonPath(responsePath = "forecastDate")
    private String forecastDate;

    @JsonPath(responsePath = "meta/search_desktop_banners")
    private String searchDesktopBanners;
    @JsonPath(responsePath = "meta/search_mobile_banners")
    private String searchMobileBanners;
    @JsonPath(responsePath = "meta/context_poster_banners")
    private String contextPosterBanners;
    @JsonPath(responsePath = "meta/context_banners")
    private String contextBanners;
    @JsonPath(responsePath = "meta/total_banners")
    private String totalBanners;

    @JsonPath(responsePath = "banners/can_delete_banner")
    private List<String> canDeleteBanners;
    @JsonPath(responsePath = "banners/can_archive_banner")
    private List<String> canArchiveBanners;
    @JsonPath(responsePath = "banners/bid")
    private List<String> bids;


    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getPriorityID() {
        return priorityID;
    }

    public void setPriorityID(String priorityID) {
        this.priorityID = priorityID;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public void setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public void setStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
    }

    public String getStatusPostModerate() {
        return statusPostModerate;
    }

    public void setStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
    }

    public String getStatusShowsForecast() {
        return statusShowsForecast;
    }

    public void setStatusShowsForecast(String statusShowsForecast) {
        this.statusShowsForecast = statusShowsForecast;
    }

    public String getStatusAutobudgetShow() {
        return statusAutobudgetShow;
    }

    public void setStatusAutobudgetShow(String statusAutobudgetShow) {
        this.statusAutobudgetShow = statusAutobudgetShow;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public Integer getMobileMultiplierPct() {
        return mobileMultiplierPct;
    }

    public void setMobileMultiplierPct(Integer mobileMultiplierPct) {
        this.mobileMultiplierPct = mobileMultiplierPct;
    }

    public String getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(String forecastDate) {
        this.forecastDate = forecastDate;
    }

    public String getSearchDesktopBanners() {
        return searchDesktopBanners;
    }

    public void setSearchDesktopBanners(String searchDesktopBanners) {
        this.searchDesktopBanners = searchDesktopBanners;
    }

    public String getSearchMobileBanners() {
        return searchMobileBanners;
    }

    public void setSearchMobileBanners(String searchMobileBanners) {
        this.searchMobileBanners = searchMobileBanners;
    }

    public String getContextPosterBanners() {
        return contextPosterBanners;
    }

    public void setContextPosterBanners(String contextPosterBanners) {
        this.contextPosterBanners = contextPosterBanners;
    }

    public String getContextBanners() {
        return contextBanners;
    }

    public void setContextBanners(String contextBanners) {
        this.contextBanners = contextBanners;
    }

    public String getTotalBanners() {
        return totalBanners;
    }

    public void setTotalBanners(String totalBanners) {
        this.totalBanners = totalBanners;
    }

    public List<String> getCanDeleteBanners() {
        return canDeleteBanners;
    }

    public void setCanDeleteBanners(List<String> canDeleteBanners) {
        this.canDeleteBanners = canDeleteBanners;
    }

    public List<String> getCanArchiveBanners() {
        return canArchiveBanners;
    }

    public void setCanArchiveBanners(List<String> canArchiveBanners) {
        this.canArchiveBanners = canArchiveBanners;
    }

    public List<String> getBids() {
        return bids;
    }

    public void setBids(List<String> bids) {
        this.bids = bids;
    }

    public GetAdGroupResponse() {
    }

    public GetAdGroupResponse(String pid, String priorityID, String geo, String groupName, String statusModerate, String statusBsSynced, String statusPostModerate, String statusShowsForecast, String statusAutobudgetShow, Integer cid, Integer mobileMultiplierPct, String forecastDate, String searchDesktopBanners, String searchMobileBanners, String contextPosterBanners, String contextBanners, String totalBanners, List<String> canDeleteBanners, List<String> canArchiveBanners, List<String> bids) {
        this.pid = pid;
        this.priorityID = priorityID;
        this.geo = geo;
        this.groupName = groupName;
        this.statusModerate = statusModerate;
        this.statusBsSynced = statusBsSynced;
        this.statusPostModerate = statusPostModerate;
        this.statusShowsForecast = statusShowsForecast;
        this.statusAutobudgetShow = statusAutobudgetShow;
        this.cid = cid;
        this.mobileMultiplierPct = mobileMultiplierPct;
        this.forecastDate = forecastDate;
        this.searchDesktopBanners = searchDesktopBanners;
        this.searchMobileBanners = searchMobileBanners;
        this.contextPosterBanners = contextPosterBanners;
        this.contextBanners = contextBanners;
        this.totalBanners = totalBanners;
        this.canDeleteBanners = canDeleteBanners;
        this.canArchiveBanners = canArchiveBanners;
        this.bids = bids;
    }
}
