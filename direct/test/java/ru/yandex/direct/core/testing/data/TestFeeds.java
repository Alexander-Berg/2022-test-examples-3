package ru.yandex.direct.core.testing.data;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.MasterSystem;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsBase64YaStringWithoutPadding;

public class TestFeeds {

    static final FeedType DEFAULT_FEED_TYPE = FeedType.YANDEX_MARKET;
    static final BusinessType DEFAULT_FEED_BUSINESS_TYPE = BusinessType.RETAIL;
    static final Source DEFAULT_FEED_SOURCE = Source.URL;
    public static final String DEFAULT_FEED_URL = "https://yandex.st/market-export/1.0-17/partner/help/YML.xml";
    public static final String DEFAULT_FEED_SITE = "https://site.yandex.net/";
    public static final byte[] FILE_DATA = "<yml_catalog />".getBytes(StandardCharsets.UTF_8);
    public static final String FILE_HASH = getMd5HashAsBase64YaStringWithoutPadding(FILE_DATA);

    private TestFeeds() {
    }

    public static Feed defaultFeed() {
        return defaultFeed(null);
    }

    public static Feed defaultFeed(@Nullable ClientId clientId) {
        return new Feed().withClientId(clientId != null ? clientId.asLong() : null)
                .withSource(DEFAULT_FEED_SOURCE)
                .withUrl(DEFAULT_FEED_URL)
                .withName("UrlFeedForTest")
                .withRefreshInterval(10L)
                .withUpdateStatus(UpdateStatus.DONE)
                .withFeedType(DEFAULT_FEED_TYPE)
                .withBusinessType(DEFAULT_FEED_BUSINESS_TYPE)
                .withMasterSystem(MasterSystem.DIRECT)
                .withLogin("login")
                .withPlainPassword("password");
    }

    public static Feed defaultFileFeed() {
        return defaultFileFeed(null);
    }

    public static Feed defaultFileFeed(@Nullable ClientId clientId) {
        return new Feed().withClientId(clientId != null ? clientId.asLong() : null)
                .withSource(Source.FILE)
                .withName("FileFeedForTest")
                .withUpdateStatus(UpdateStatus.DONE)
                .withFeedType(DEFAULT_FEED_TYPE)
                .withBusinessType(DEFAULT_FEED_BUSINESS_TYPE)
                .withFilename("test.tsv")
                .withCachedFileHash(FILE_HASH)
                .withRefreshInterval(0L);
    }

    public static Feed defaultSiteFeed() {
        return defaultSiteFeed(null);
    }

    public static Feed defaultSiteFeed(@Nullable ClientId clientId) {
        return new Feed().withClientId(clientId != null ? clientId.asLong() : null)
                .withSource(Source.SITE)
                .withName("SiteFeedForTest")
                .withUrl(DEFAULT_FEED_SITE)
                .withUpdateStatus(UpdateStatus.DONE)
                .withFeedType(DEFAULT_FEED_TYPE)
                .withBusinessType(DEFAULT_FEED_BUSINESS_TYPE)
                .withRefreshInterval(0L);
    }

}
