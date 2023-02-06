package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

public class SaveAdGroupTagsParameters extends BasicDirectFormParameters {

    @FormParameter("new_tags")
    private String newTags;
    @FormParameter("cid")
    private String cid;
    @FormParameter("adgroup_ids")
    private String adgroupIds;
    @FormParameter("tagIds")
    private String tagIds;

    public String getNewTags() {
        return newTags;
    }

    public void setNewTags(String newTags) {
        this.newTags = newTags;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public void setAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
    }

    public String getTagIds() {
        return tagIds;
    }

    public void setTagIds(String tagIds) {
        this.tagIds = tagIds;
    }
}
