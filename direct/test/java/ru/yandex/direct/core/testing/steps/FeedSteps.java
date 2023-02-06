package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.feed.FeedUtilsKt;
import ru.yandex.direct.core.entity.feed.container.FeedQueryFilter;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedCategory;
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItem;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.MasterSystem;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.StatusMBIEnabled;
import ru.yandex.direct.core.entity.feed.model.StatusMBISynced;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedDefectIdsEnum;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.repository.TestFeedCategoryRepository;
import ru.yandex.direct.core.testing.repository.TestFeedHistoryRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.utils.model.UrlParts;
import ru.yandex.misc.random.Random2;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.createFakeFeedUrl;
import static ru.yandex.direct.core.entity.feed.validation.constraints.FeedConstraints.MBI_MAX_LOGIN_LENGTH;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFileFeed;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultSiteFeed;

public class FeedSteps {

    private final ShardHelper shardHelper;
    private final ClientSteps clientSteps;
    private final FeedRepository feedRepository;
    private final TestFeedCategoryRepository categoryRepository;
    private final TestFeedHistoryRepository historyRepository;

    public FeedSteps(ShardHelper shardHelper,
                     ClientSteps clientSteps,
                     FeedRepository feedRepository,
                     TestFeedCategoryRepository categoryRepository,
                     TestFeedHistoryRepository historyRepository) {
        this.shardHelper = shardHelper;
        this.clientSteps = clientSteps;
        this.feedRepository = feedRepository;
        this.categoryRepository = categoryRepository;
        this.historyRepository = historyRepository;
    }

    public FeedInfo createDefaultFeed() {
        return createFeed(new FeedInfo());
    }

    public FeedInfo createDefaultFeed(ClientInfo clientInfo) {
        return createFeed(new FeedInfo().withClientInfo(clientInfo));
    }

    public FeedInfo createDefaultFileFeed(ClientInfo clientInfo) {
        return createFeed(
                new FeedInfo()
                        .withClientInfo(clientInfo)
                        .withFeed(defaultFileFeed(clientInfo.getClientId()))
        );
    }

    public FeedInfo createFeed(ClientInfo clientInfo, FeedType feedType, BusinessType businessType) {
        FeedInfo feedInfo = new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withFeedType(feedType)
                        .withBusinessType(businessType))
                .withClientInfo(clientInfo);
        return createFeed(feedInfo);
    }

    public FeedInfo createFeed(ClientInfo clientInfo, FeedType feedType, BusinessType businessType, Source source) {
        FeedInfo feedInfo = new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withFeedType(feedType)
                        .withBusinessType(businessType)
                        .withSource(source))
                .withClientInfo(clientInfo);
        return createFeed(feedInfo);
    }

    public FeedInfo createFeed(ClientInfo clientInfo, UpdateStatus updateStatus) {
        FeedInfo feedInfo = new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withUpdateStatus(updateStatus))
                .withClientInfo(clientInfo);
        return createFeed(feedInfo);
    }

    public FeedInfo createFeed(ClientInfo clientInfo, String targetDomain) {
        FeedInfo feedInfo = new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withTargetDomain(targetDomain))
                .withClientInfo(clientInfo);
        return createFeed(feedInfo);
    }

    public FeedInfo createFeed(FeedInfo feedInfo) {
        ClientInfo clientInfo = feedInfo.getClientInfo();
        if (clientInfo == null) {
            clientInfo = clientSteps.createDefaultClient();
            feedInfo.withClientInfo(clientInfo);
        }
        Feed feed = feedInfo.getFeed() != null ? feedInfo.getFeed() : defaultFeed(null);
        if (feed.getId() == null) {
            feed.setClientId(clientInfo.getClientId().asLong());
            feedRepository.add(clientInfo.getShard(), singleton(feed));
            feed = feedRepository.get(clientInfo.getShard(), singleton(feed.getId())).get(0);
            // Иначе, при попытке сверить этот фид в matchedBy(beanDiffer(...)) без специальной обработки поля с датой
            // получаем мигающие тесты (что, собственно, и произошло).
            feed.setLastChange(null);
            feedInfo.setFeed(feed);
        }
        return feedInfo;
    }

    public FeedInfo createDefaultSyncedFeed(ClientInfo clientInfo) {
        Long feedId = shardHelper.generateFeedIds(1).get(0);

        // разные, но связанные сущности, эмулируем уникальность добавляя смещения
        Long marketFeedId = feedId + 1_000_000;
        Long marketShopId = feedId + 5_000_000;
        Long businessId = clientInfo.getClientId().asLong() + 1_000_000L;

        Feed newFeed = defaultFeed(clientInfo.getClientId())
                .withId(feedId)
                .withMarketBusinessId(businessId)
                .withMarketShopId(marketShopId)
                .withMarketFeedId(marketFeedId)
                .withRefreshInterval(0L)
                .withStatusMbiSynced(StatusMBISynced.YES)
                .withStatusMbiEnabled(StatusMBIEnabled.YES);
        String fakeUrl = FeedUtilsKt.createFakeFeedUrl(businessId, marketShopId, marketFeedId, newFeed.getUrl(),
                true, false);
        newFeed.setUrl(fakeUrl);

        feedRepository.add(clientInfo.getShard(), singletonList(newFeed));
        Feed feed = feedRepository.get(clientInfo.getShard(), clientInfo.getClientId(), singletonList(feedId)).get(0);

        return new FeedInfo()
                .withClientInfo(clientInfo)
                .withFeed(feed);
    }

    public FeedInfo createDefaultSyncedSiteFeed(ClientInfo clientInfo) {
        Long feedId = shardHelper.generateFeedIds(1).get(0);

        // разные, но связанные сущности, эмулируем уникальность добавляя смещения
        Long marketFeedId = feedId + 1_000_000;
        Long marketShopId = feedId + 5_000_000;
        Long businessId = clientInfo.getClientId().asLong() + 1_000_000L;

        Feed newFeed = defaultSiteFeed(clientInfo.getClientId())
                .withId(feedId)
                .withMarketFeedId(marketFeedId)
                .withMarketBusinessId(businessId)
                .withMarketShopId(marketShopId)
                .withRefreshInterval(0L);
        UrlParts urlParts = UrlParts.fromUrl(newFeed.getUrl());
        String fakeUrl = FeedUtilsKt.createFakeFeedUrl(businessId, marketShopId, marketFeedId, urlParts.getProtocol(),
                urlParts.getDomain());
        newFeed.setUrl(fakeUrl);

        feedRepository.add(clientInfo.getShard(), singletonList(newFeed));
        Feed feed = feedRepository.get(clientInfo.getShard(), clientInfo.getClientId(), singletonList(feedId)).get(0);

        return new FeedInfo()
                .withClientInfo(clientInfo)
                .withFeed(feed);
    }

    public void createFeedCategory(int shard, FeedCategory category) {
        categoryRepository.add(shard, category);
    }

    public void createFeedHistoryItem(int shard, FeedHistoryItem historyItem) {
        historyRepository.add(shard, historyItem);
    }

    public <V> void setFeedProperty(FeedInfo feedInfo, ModelProperty<? super Feed, V> property, V value) {
        if (!Feed.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + Feed.class.getName() + " doesn't contain property " + property.name());
        }
        Feed feed = feedInfo.getFeed();
        AppliedChanges<Feed> appliedChanges = new ModelChanges<>(feed.getId(), Feed.class)
                .process(value, property)
                .applyTo(feed);
        feedRepository.update(feedInfo.getShard(), singletonList(appliedChanges));
    }

    public void deleteFeed(int shard, List<Long> feedIds) {
        feedRepository.delete(shard, feedIds);
    }

    public FeedInfo processFeed(ClientInfo clientInfo, Long feedId, UpdateStatus updateStatus,
                                @Nullable FeedDefectIdsEnum defect) {
        ClientId clientId = clientInfo.getClientId();
        int shard = shardHelper.getShardByClientId(clientId);
        FeedQueryFilter filter = FeedQueryFilter.newBuilder().withFeedIds(Set.of(feedId)).build();
        Feed feed = feedRepository.get(shard, clientId, filter).get(0);
        FeedInfo feedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(feed);

        setFeedProperty(feedInfo, Feed.UPDATE_STATUS, updateStatus);

        if (updateStatus != UpdateStatus.ERROR || defect == null) {
            return feedInfo;
        }

        switch (defect) {
            case FEED_NAME_CANNOT_BE_EMPTY:
                setFeedProperty(feedInfo, Feed.NAME, "");
                return feedInfo;
            case FEED_INVALID_HREF:
                setFeedProperty(feedInfo, Feed.URL, "invalid.url");
                return feedInfo;
            case URL_CANNOT_BE_NULL:
                setFeedProperty(feedInfo, Feed.SOURCE, Source.URL);
                setFeedProperty(feedInfo, Feed.URL, null);
                return feedInfo;
            case URL_IS_FAKE:
                String fakeFeedUrl = createFakeFeedUrl(1L, 2L, 3L, "http://ya.ru", false, false);
                setFeedProperty(feedInfo, Feed.MASTER_SYSTEM, MasterSystem.DIRECT);
                setFeedProperty(feedInfo, Feed.URL, fakeFeedUrl);
                return feedInfo;
            case FILE_DATA_CANNOT_BE_NULL:
                setFeedProperty(feedInfo, Feed.SOURCE, Source.FILE);
                setFeedProperty(feedInfo, Feed.FILE_DATA, null);
                return feedInfo;
            case FEED_INVALID_FILENAME:
                setFeedProperty(feedInfo, Feed.FILENAME, "invalid.jpg");
                return feedInfo;
            case FEED_LOGIN_IS_NOT_SET:
                setFeedProperty(feedInfo, Feed.PLAIN_PASSWORD, "password");
                setFeedProperty(feedInfo, Feed.LOGIN, null);
                return feedInfo;
            case FEED_LOGIN_CANNOT_BE_EMPTY:
                setFeedProperty(feedInfo, Feed.PLAIN_PASSWORD, "password");
                setFeedProperty(feedInfo, Feed.LOGIN, "");
                return feedInfo;
            case LOGIN_LENGTH_CANNOT_BE_MORE_THAN_MAX:
                setFeedProperty(feedInfo, Feed.PLAIN_PASSWORD, "password");
                setFeedProperty(feedInfo, Feed.LOGIN, Random2.R.nextStringSimpleUtf8(MBI_MAX_LOGIN_LENGTH + 1));
                return feedInfo;
            case FEED_PASSWORD_IS_NOT_SET:
                setFeedProperty(feedInfo, Feed.LOGIN, "login");
                setFeedProperty(feedInfo, Feed.PLAIN_PASSWORD, null);
                return feedInfo;
            case FEED_PASSWORD_CANNOT_BE_EMPTY:
                setFeedProperty(feedInfo, Feed.LOGIN, "login");
                setFeedProperty(feedInfo, Feed.PLAIN_PASSWORD, "");
                return feedInfo;
            case EMAIL_INVALID_VALUE:
                setFeedProperty(feedInfo, Feed.EMAIL, "invalidEmail");
                return feedInfo;
            case SHOP_NAME_CANNOT_BE_NULL:
                setFeedProperty(feedInfo, Feed.MASTER_SYSTEM, MasterSystem.MARKET);
                setFeedProperty(feedInfo, Feed.SHOP_NAME, null);
                return feedInfo;
            case SHOP_NAME_CANNOT_BE_EMPTY:
                setFeedProperty(feedInfo, Feed.MASTER_SYSTEM, MasterSystem.MARKET);
                setFeedProperty(feedInfo, Feed.SHOP_NAME, "");
                return feedInfo;
            default:
                throw new IllegalArgumentException("Process of feed defect " + defect + " is not implemented yet");
        }
    }
}
