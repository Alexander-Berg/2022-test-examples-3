package ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;

import java.util.List;

public class EditAdGroupsPerformanceResponse {

    @SerializedName("campaign")
    private PerformanceCampaign campaign;

    @SerializedName("feeds")
    private List<Feed> feeds;

    public PerformanceCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(PerformanceCampaign campaign) {
        this.campaign = campaign;
    }

    public List<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<Feed> feeds) {
        this.feeds = feeds;
    }
}
