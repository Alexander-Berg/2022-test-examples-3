package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by aleran on 02.09.2015.
 */
public class AjaxFeedsResponse {

    @SerializedName("result")
    private FeedsResult feedsResult;

    public FeedsResult getFeedsResult() {
        return feedsResult;
    }

    public void setFeedsResult(FeedsResult feedsResult) {
        this.feedsResult = feedsResult;
    }

    public class FeedsResult {
        @SerializedName("items")
        List<Feed> feeds;

        public List<Feed> getFeeds() {
            return feeds;
        }

        public void setFeeds(List<Feed> feeds) {
            this.feeds = feeds;
        }
    }
}
