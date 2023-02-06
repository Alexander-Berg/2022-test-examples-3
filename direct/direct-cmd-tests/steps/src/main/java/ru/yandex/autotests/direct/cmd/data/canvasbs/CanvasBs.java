package ru.yandex.autotests.direct.cmd.data.canvasbs;

import com.google.gson.annotations.SerializedName;

public class CanvasBs {

    @SerializedName("creative_id")
    private Long creativeId;

    @SerializedName("creative_name")
    private String creativeName;

    @SerializedName("width")
    private Integer width;

    @SerializedName("height")
    private Integer height;

    @SerializedName("preview_url")
    private String previewUrl;

    @SerializedName("live_preview_url")
    private String livePreviewUrl;

    @SerializedName("moderation_info")
    private Object moderationInfo;

    public Long getCreativeId() {
        return creativeId;
    }

    public CanvasBs withCreativeId(Long creativeId) {
        this.creativeId = creativeId;
        return this;
    }

    public String getCreativeName() {
        return creativeName;
    }

    public CanvasBs withCreativeName(String creativeName) {
        this.creativeName = creativeName;
        return this;
    }

    public Integer getWidth() {
        return width;
    }

    public CanvasBs withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public Integer getHeight() {
        return height;
    }

    public CanvasBs withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public CanvasBs withPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
        return this;
    }

    public String getLivePreviewUrl() {
        return livePreviewUrl;
    }

    public CanvasBs withLivePreviewUrl(String livePreviewUrl) {
        this.livePreviewUrl = livePreviewUrl;
        return this;
    }

    public Object getModerationInfo() {
        return moderationInfo;
    }

    public CanvasBs withModerationInfo(Object moderateInfo) {
        this.moderationInfo = moderateInfo;
        return this;
    }
}
