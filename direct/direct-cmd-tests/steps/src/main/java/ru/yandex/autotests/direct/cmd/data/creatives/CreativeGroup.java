package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreativeGroup {

    @SerializedName("group_id")
    private Long groupId;

    @SerializedName("creatives_data")
    private List<ShortCreative> creativesData;

    @SerializedName("group_creatives_count")
    private Integer groupCreativesCount;

    public Long getGroupId() {
        return groupId;
    }

    public CreativeGroup withGroupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }

    public List<ShortCreative> getCreativesData() {
        return creativesData;
    }

    public CreativeGroup withCreativesData(List<ShortCreative> creativesData) {
        this.creativesData = creativesData;
        return this;
    }

    public Integer getGroupCreativesCount() {
        return groupCreativesCount;
    }

    public CreativeGroup withGroupCreativesCount(Integer groupCreativesCount) {
        this.groupCreativesCount = groupCreativesCount;
        return this;
    }
}
