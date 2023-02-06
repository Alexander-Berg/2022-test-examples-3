package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by aleran on 15.09.2015.
 */
public class MobileContentCmdBean {

    @JsonPath(responsePath = "modelId")
    @SerializedName("modelId")
    private String modelId;

    @JsonPath(responsePath = "mobile_content_id")
    @SerializedName("mobile_content_id")
    private String mobileContentId;

    @JsonPath(responsePath = "name")
    @SerializedName("name")
    private String name;

    //@JsonPath(responsePath = "prices", requestPath = "prices")
    //private MobileContentPrices prices;

    @JsonPath(responsePath = "rating", requestPath = "rating")
    @SerializedName("rating")
    private String rating;

    @JsonPath(responsePath = "rating_votes", requestPath = "rating_votes")
    @SerializedName("rating_votes")
    private String ratingVotes;

    @JsonPath(responsePath = "icon_url", requestPath = "icon_url")
    @SerializedName("icon_url")
    private String iconUrl;

    @JsonPath(responsePath = "os_type", requestPath = "os_type")
    @SerializedName("os_type")
    private String osType;

    @JsonPath(responsePath = "min_os_version", requestPath = "min_os_version")
    @SerializedName("min_os_version")
    private String minOsVersion;

    @JsonPath(responsePath = "available_actions", requestPath = "available_actions")
    @SerializedName("available_actions")
    private List<String> availableActions;

    @JsonPath(responsePath = "is_available", requestPath = "is_available")
    @SerializedName("is_available")
    private String isAvailable;

    @JsonPath(responsePath = "category", requestPath = "category")
    @SerializedName("category")
    private String category;

    @JsonPath(responsePath = "age_label", requestPath = "age_label")
    @SerializedName("age_label")
    private String ageLabel;

    @JsonPath(responsePath = "is_show_icon", requestPath = "is_show_icon")
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
