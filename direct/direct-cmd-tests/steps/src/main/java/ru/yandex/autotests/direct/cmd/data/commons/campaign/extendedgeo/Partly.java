package ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import static java.util.Arrays.asList;

public class Partly {
    @SerializedName("adgroup_ids")
    private List<String> groupIds;

    public List<String> getGroupIds() {
        return groupIds;
    }

    public Partly withGroupIds(String... groupIds) {
        this.groupIds = asList(groupIds);
        return this;
    }
}
