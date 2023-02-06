package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.dbutil.model.BusinessIdAndShopId;
import ru.yandex.direct.dbutil.model.ClientId;

public class FeedInfo {

    private ClientInfo clientInfo;
    private Feed feed;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public FeedInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public FeedInfo withFeed(Feed feed) {
        this.feed = feed;
        return this;
    }

    public Long getFeedId() {
        return getFeed().getId();
    }

    public BusinessIdAndShopId getBusinessIdAndShopId() {
        return BusinessIdAndShopId.ofNullable(feed.getMarketBusinessId(), feed.getMarketShopId());
    }

    public Long getBusinessId() {
        return feed.getMarketBusinessId();
    }

    public Long getShopId() {
        return feed.getMarketShopId();
    }

    public Long getMarketFeedId() {
        return feed.getMarketFeedId();
    }

    public Long getUid() {
        return getClientInfo().getUid();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }

}
