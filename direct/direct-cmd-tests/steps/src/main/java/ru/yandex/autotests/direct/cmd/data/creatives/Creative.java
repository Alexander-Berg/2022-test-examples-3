package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Creative {

    @SerializedName("creative_id")
    private Long creativeId;
    @SerializedName("client_id")
    private Long clientId;

    @SerializedName("status_moderate")
    private String statusModerate;
    @SerializedName("moderate_try_count")
    private Integer moderateTryCount;

    @SerializedName("moderate_send_time")
    private String moderateSendTime;
    @SerializedName("name")
    private String name;
    @SerializedName("preview_url")
    private String previewUrl;
    @SerializedName("template_id")
    private Long templateId;
    @SerializedName("href")
    private String href;
    @SerializedName("width")
    private Integer width;
    @SerializedName("height")
    private Integer height;
    @SerializedName("alt_text")
    private String altText;
    @SerializedName("bs_template_name")
    private String bsTemplateName;
    @SerializedName("sum_geo")
    private String sumGeo;
    @SerializedName("used_in_camps")
    private List<CreativeCamp> usedInCamps;
    @SerializedName("business_type")
    private String businessType;
    @SerializedName("feed_type")
    private String feedType;
    @SerializedName("creative_group_id")
    private Long creativeGroupId;

    public Long getCreativeId() {
        return creativeId;
    }

    public Creative withCreativeId(Long creativeId) {
        this.creativeId = creativeId;
        return this;
    }

    public Long getClientId() {
        return clientId;
    }

    public Creative withClientId(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public Creative withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public Integer getModerateTryCount() {
        return moderateTryCount;
    }

    public Creative withModerateTryCount(Integer moderateTryCount) {
        this.moderateTryCount = moderateTryCount;
        return this;
    }

    public String getModerateSendTime() {
        return moderateSendTime;
    }

    public Creative withModerateSendTime(String moderateSendTime) {
        this.moderateSendTime = moderateSendTime;
        return this;
    }

    public String getName() {
        return name;
    }

    public Creative withName(String name) {
        this.name = name;
        return this;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public Creative withPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
        return this;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Creative withTemplateId(Long templateId) {
        this.templateId = templateId;
        return this;
    }

    public String getHref() {
        return href;
    }

    public Creative withHref(String href) {
        this.href = href;
        return this;
    }

    public Integer getWidth() {
        return width;
    }

    public Creative withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public Integer getHeight() {
        return height;
    }

    public Creative withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public String getAltText() {
        return altText;
    }

    public Creative withAltText(String altText) {
        this.altText = altText;
        return this;
    }

    public String getBsTemplateName() {
        return bsTemplateName;
    }

    public Creative withBsTemplateName(String bsTemplateName) {
        this.bsTemplateName = bsTemplateName;
        return this;
    }

    public String getSumGeo() {
        return sumGeo;
    }

    public Creative withSumGeo(String sumGeo) {
        this.sumGeo = sumGeo;
        return this;
    }

    public List<CreativeCamp> getUsedInCamps() {
        return usedInCamps;
    }

    public Creative withUsedInCamps(List<CreativeCamp> usedInCamps) {
        this.usedInCamps = usedInCamps;
        return this;
    }

    public String getBusinessType() {
        return businessType;
    }

    public Creative withBusinessType(String businessType) {
        this.businessType = businessType;
        return this;
    }

    public String getFeedType() {
        return feedType;
    }

    public Creative withFeedType(String feedType) {
        this.feedType = feedType;
        return this;
    }

    public Long getCreativeGroupId() {
        return creativeGroupId;
    }

    public Creative withCreativeGroupId(Long creativeGroupId) {
        this.creativeGroupId = creativeGroupId;
        return this;
    }
}