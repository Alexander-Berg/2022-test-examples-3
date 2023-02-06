package ru.yandex.autotests.direct.cmd.data.performanceGroups.common;

import com.google.gson.annotations.SerializedName;

public class PerformanceBanner {

    @SerializedName("bid")
    private String bid;

    @SerializedName("isNewBanner")
    private boolean isNewBanner;

    @SerializedName("banner_type")
    private String bannerType;

    @SerializedName("statusShow")
    private String statusShow;

    @SerializedName("statusModerate")
    private String statusModerate;

    @SerializedName("archive")
    private String archive;

    @SerializedName("can_delete_banner")
    private String canDeleteBanner;

    @SerializedName("can_archive_banner")
    private String canArchiveBanner;

    @SerializedName("creative")
    private CreativeBanner creativeBanner;

    @SerializedName("newBannerIndex")
    private String newBannerIndex;

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public boolean isNewBanner() {
        return isNewBanner;
    }

    public void setIsNewBanner(boolean isNewBanner) {
        this.isNewBanner = isNewBanner;
    }

    public String getBannerType() {
        return bannerType;
    }

    public void setBannerType(String bannerType) {
        this.bannerType = bannerType;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public void setStatusShow(String statusShow) {
        this.statusShow = statusShow;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public void setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
    }

    public String getArchive() {
        return archive;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getCanDeleteBanner() {
        return canDeleteBanner;
    }

    public void setCanDeleteBanner(String canDeleteBanner) {
        this.canDeleteBanner = canDeleteBanner;
    }

    public String getCanArchiveBanner() {
        return canArchiveBanner;
    }

    public void setCanArchiveBanner(String canArchiveBanner) {
        this.canArchiveBanner = canArchiveBanner;
    }

    public CreativeBanner getCreativeBanner() {
        return creativeBanner;
    }

    public void setCreativeBanner(CreativeBanner creativeBanner) {
        this.creativeBanner = creativeBanner;
    }

    public String getNewBannerIndex() {
        return newBannerIndex;
    }

    public void setNewBannerIndex(String newBannerIndex) {
        this.newBannerIndex = newBannerIndex;
    }
}
