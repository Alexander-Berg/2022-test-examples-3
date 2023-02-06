package ru.yandex.autotests.direct.cmd.data.performanceGroups.common;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreativeBanner {

    @SerializedName("creative_id")
    private Long creativeId;

    @SerializedName("name")
    private String name;

    @SerializedName("width")
    private String width;

    @SerializedName("height")
    private String height;

    @SerializedName("alt_text")
    private String altText;

    @SerializedName("href")
    private String href;

    @SerializedName("preview_url")
    private String previewUrl;

    @SerializedName("ClientID")
    private String ClientID;

    @SerializedName("status_moderate")
    private String statusModerate;

    @SerializedName("using_in_camps")
    private List<String> usingInCamps;

    @SerializedName("reject_reasons")
    private List<String> rejectReasons;

    @SerializedName("sizeTag")
    private String sizeTag;

    public Long getCreativeId() {
        return creativeId;
    }

    public void setCreativeId(Long creativeId) {
        this.creativeId = creativeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getClientID() {
        return ClientID;
    }

    public void setClientID(String clientID) {
        ClientID = clientID;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public void setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
    }

    public List<String> getUsingInCamps() {
        return usingInCamps;
    }

    public void setUsingInCamps(List<String> usingInCamps) {
        this.usingInCamps = usingInCamps;
    }

    public List<String> getRejectReasons() {
        return rejectReasons;
    }

    public void setRejectReasons(List<String> rejectReasons) {
        this.rejectReasons = rejectReasons;
    }

    public String getSizeTag() {
        return sizeTag;
    }

    public void setSizeTag(String sizeTag) {
        this.sizeTag = sizeTag;
    }

    public CreativeBanner withCreativeId(Long creativeId) {
        this.creativeId = creativeId;
        return this;
    }

    public CreativeBanner withName(String name) {
        this.name = name;
        return this;
    }

    public CreativeBanner withWidth(String width) {
        this.width = width;
        return this;
    }

    public CreativeBanner withHeight(String height) {
        this.height = height;
        return this;
    }

    public CreativeBanner withAltText(String altText) {
        this.altText = altText;
        return this;
    }

    public CreativeBanner withHref(String href) {
        this.href = href;
        return this;
    }

    public CreativeBanner withPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
        return this;
    }

    public CreativeBanner withClientID(String clientID) {
        ClientID = clientID;
        return this;
    }

    public CreativeBanner withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public CreativeBanner withUsingInCamps(List<String> usingInCamps) {
        this.usingInCamps = usingInCamps;
        return this;
    }

    public CreativeBanner withRejectReasons(List<String> rejectReasons) {
        this.rejectReasons = rejectReasons;
        return this;
    }

    public CreativeBanner withSizeTag(String sizeTag) {
        this.sizeTag = sizeTag;
        return this;
    }
}
