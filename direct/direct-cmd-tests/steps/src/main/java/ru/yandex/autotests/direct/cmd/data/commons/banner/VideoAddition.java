package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;

public class VideoAddition {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("resources_url")
    private String resourcesUrl;

    @SerializedName("resource_type")
    private VideoAdditionResourceType resourceType;

    @SerializedName("status_moderate")
    private StatusModerate statusModerate;

    public StatusModerate getStatusModerate() {
        return statusModerate;
    }

    public static VideoAddition getDefaultVideoAddition(Long creativeId) {
        return new VideoAddition()
                .withId(creativeId)
                .withName("тестовое видео дополнение")
                .withResourcesUrl("https://ya.ru/")
                .withResourceType(VideoAdditionResourceType.CREATIVE);
    }

    public void setStatusModerate(
            StatusModerate statusModerate)
    {
        this.statusModerate = statusModerate;
    }

    public VideoAddition withStatusModerate(
            StatusModerate statusModerate)
    {
        setStatusModerate(statusModerate);
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VideoAddition withId(Long id) {
        setId(id);
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VideoAddition withName(String name) {
        setName(name);
        return this;
    }

    public String getResourcesUrl() {
        return resourcesUrl;
    }

    public void setResourcesUrl(String resourcesUrl) {
        this.resourcesUrl = resourcesUrl;
    }

    public VideoAddition withResourcesUrl(String resourcesUrl) {
        setResourcesUrl(resourcesUrl);
        return this;
    }

    public VideoAdditionResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(VideoAdditionResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public VideoAddition withResourceType(VideoAdditionResourceType resourceType) {
        setResourceType(resourceType);
        return this;
    }
}
