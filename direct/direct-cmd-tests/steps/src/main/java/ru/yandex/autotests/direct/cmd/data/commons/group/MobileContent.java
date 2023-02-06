package ru.yandex.autotests.direct.cmd.data.commons.group;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MobileContent {

    @SerializedName("modelId")
    private String modelId;

    @SerializedName("mobile_content_id")
    private String mobileContentId;

    @SerializedName("name")
    private String name;

    //@JsonPath(responsePath = "prices", requestPath = "prices")
    //private MobileContentPrices prices;

    @SerializedName("rating")
    private String rating;

    @SerializedName("rating_votes")
    private String ratingVotes;

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("os_type")
    private String osType;

    @SerializedName("min_os_version")
    private String minOsVersion;

    @SerializedName("available_actions")
    private List<String> availableActions;

    @SerializedName("is_available")
    private String isAvailable;

    @SerializedName("category")
    private String category;

    @SerializedName("age_label")
    private String ageLabel;

    @SerializedName("is_show_icon")
    private String isShowIcon;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getMobileContentId() {
        return mobileContentId;
    }

    public void setMobileContentId(String mobileContentId) {
        this.mobileContentId = mobileContentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getRatingVotes() {
        return ratingVotes;
    }

    public void setRatingVotes(String ratingVotes) {
        this.ratingVotes = ratingVotes;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getMinOsVersion() {
        return minOsVersion;
    }

    public void setMinOsVersion(String minOsVersion) {
        this.minOsVersion = minOsVersion;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }

    public void setAvailableActions(List<String> availableActions) {
        this.availableActions = availableActions;
    }

    public String getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(String isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAgeLabel() {
        return ageLabel;
    }

    public void setAgeLabel(String ageLabel) {
        this.ageLabel = ageLabel;
    }

    public String getIsShowIcon() {
        return isShowIcon;
    }

    public void setIsShowIcon(String isShowIcon) {
        this.isShowIcon = isShowIcon;
    }
}
