package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

public class FeedHistory {

    @SerializedName("id")
    private String id;

    @SerializedName("feed_id")
    private String feedId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("parse_results")
    private FeedParseResults parseResults;

    @SerializedName("cached_file_url")
    private String cachedFileUrl;

    @SerializedName("offers_count")
    private String offers_count;

    public String getId() {
        return id;
    }

    public FeedHistory withId(String id) {
        this.id = id;
        return this;
    }

    public String getFeedId() {
        return feedId;
    }

    public FeedHistory withFeedId(String feedId) {
        this.feedId = feedId;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public FeedHistory withCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public FeedParseResults getParseResults() {
        return parseResults;
    }

    public FeedHistory withParseResults(FeedParseResults parseResults) {
        this.parseResults = parseResults;
        return this;
    }

    public String getCachedFileUrl() {
        return cachedFileUrl;
    }

    public FeedHistory withCachedFileUrl(String cachedFileUrl) {
        this.cachedFileUrl = cachedFileUrl;
        return this;
    }

    public String getOffers_count() {
        return offers_count;
    }

    public FeedHistory withOffers_count(String offers_count) {
        this.offers_count = offers_count;
        return this;
    }
}
