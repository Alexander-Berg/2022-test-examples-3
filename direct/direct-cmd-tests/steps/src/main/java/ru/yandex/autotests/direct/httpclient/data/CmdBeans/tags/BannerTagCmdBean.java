package ru.yandex.autotests.direct.httpclient.data.CmdBeans.tags;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 19.03.15.
 */
public class BannerTagCmdBean {

    @JsonPath(responsePath = "value")
    private String value;

    @JsonPath(responsePath = "tag_id")
    private String tagId;

    @JsonPath(responsePath = "uses_count")
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
}
