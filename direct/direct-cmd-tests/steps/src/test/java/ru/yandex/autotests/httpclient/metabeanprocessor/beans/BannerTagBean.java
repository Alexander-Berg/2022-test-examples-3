package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 19.03.15.
 */
public class BannerTagBean {

    @JsonPath(requestPath = "value")
    private String value;

    @JsonPath(requestPath = "tag_id")
    private String tagId;

    @JsonPath(requestPath = "uses_count")
    private String usesCount;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getUsesCount() {
        return usesCount;
    }

    public void setUsesCount(String usesCount) {
        this.usesCount = usesCount;
    }

    public BannerTagBean() {
    }

    public BannerTagBean(String value, String tagId, String usesCount) {
        this.value = value;
        this.tagId = tagId;
        this.usesCount = usesCount;
    }
}
