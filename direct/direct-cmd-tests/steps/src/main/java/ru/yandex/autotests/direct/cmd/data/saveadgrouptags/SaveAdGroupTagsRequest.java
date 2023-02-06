package ru.yandex.autotests.direct.cmd.data.saveadgrouptags;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SaveAdGroupTagsRequest extends BasicDirectRequest {
    @SerializeKey("new_tags")
    private String newTags;
    @SerializeKey("cid")
    private String cid;
    @SerializeKey("adgroup_ids")
    private String adgroupIds;
    @SerializeKey("tagIds")
    private String tagIds;

    public String getNewTags() {
        return newTags;
    }

    public SaveAdGroupTagsRequest withNewTags(String newTags) {
        this.newTags = newTags;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public SaveAdGroupTagsRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public SaveAdGroupTagsRequest withAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
        return this;
    }

    public String getTagIds() {
        return tagIds;
    }

    public SaveAdGroupTagsRequest withTagIds(String tagIds) {
        this.tagIds = tagIds;
        return this;
    }
}
