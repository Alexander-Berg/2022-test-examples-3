package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AjaxGetFeedHistoryResult {

    @SerializedName("feed")
    private Feed feed;

    @SerializedName("feed_history")
    private List<FeedHistory> feedHistory;

    public Feed getFeed() {
        return feed;
    }

    public AjaxGetFeedHistoryResult withFeed(Feed feed) {
        this.feed = feed;
        return this;
    }

    public List<FeedHistory> getFeedHistory() {
        return feedHistory;
    }

    public AjaxGetFeedHistoryResult withFeedHistory(List<FeedHistory> feedHistory) {
        this.feedHistory = feedHistory;
        return this;
    }

}
