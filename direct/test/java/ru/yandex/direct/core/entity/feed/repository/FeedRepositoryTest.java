package ru.yandex.direct.core.entity.feed.repository;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feed.container.FeedQueryFilter;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.StatusMBISynced;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private FeedRepository feedRepository;

    private ClientInfo clientInfo;
    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void add() {
        Feed feed = defaultFeed(clientInfo.getClientId());
        List<Long> addedIds = feedRepository.add(shard, Collections.singleton(feed));
        FeedQueryFilter filter = FeedQueryFilter.newBuilder()
                .withFeedIds(addedIds)
                .build();
        List<Feed> feeds = feedRepository.get(shard, clientInfo.getClientId(), filter);
        assertThat(feeds).hasSize(1);
        assertThat(feeds.get(0)).is(matchedBy(beanDiffer(feed).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();

        ModelChanges<Feed> modelChanges = new ModelChanges<>(feedId, Feed.class);
        modelChanges.process(FeedType.YANDEX_MARKET, Feed.FEED_TYPE);
        String newUrl = "http://storage-int.mds.yandex.net/1/dfdfdsfs";
        modelChanges.process(newUrl, Feed.URL);
        modelChanges.process(BusinessType.AUTO, Feed.BUSINESS_TYPE);
        modelChanges.process(Source.FILE, Feed.SOURCE);
        String newName = "newName";
        modelChanges.process(newName, Feed.NAME);
        String newFilename = "newFileName.csv";
        modelChanges.process(newFilename, Feed.FILENAME);
        String newLogin = "newLogin";
        modelChanges.process(newLogin, Feed.LOGIN);
        String newPassword = "newPassword";
        modelChanges.process(newPassword, Feed.PLAIN_PASSWORD);
        String newEmail = "smth@ya.ru";
        modelChanges.process(newEmail, Feed.EMAIL);
        modelChanges.process(100L, Feed.REFRESH_INTERVAL);
        modelChanges.process(true, Feed.IS_REMOVE_UTM);
        modelChanges.process(UpdateStatus.UPDATING, Feed.UPDATE_STATUS);
        String newFileHash = "hashhash";
        modelChanges.process(newFileHash, Feed.CACHED_FILE_HASH);
        modelChanges.process(20L, Feed.FETCH_ERRORS_COUNT);
        modelChanges.process(1500L, Feed.OFFERS_COUNT);
        modelChanges.process(null, Feed.OFFER_EXAMPLES);
        AppliedChanges<Feed> ac = modelChanges.applyTo(feedInfo.getFeed());
        feedRepository.update(clientInfo.getShard(), List.of(ac));

        Feed expectedFeed = new Feed()
                .withFeedType(FeedType.YANDEX_MARKET)
                .withUrl(newUrl)
                .withBusinessType(BusinessType.AUTO)
                .withSource(Source.FILE)
                .withName(newName)
                .withFilename(newFilename)
                .withLogin(newLogin)
                .withPlainPassword(newPassword)
                .withEmail(newEmail)
                .withRefreshInterval(100L)
                .withIsRemoveUtm(true)
                .withUpdateStatus(UpdateStatus.UPDATING)
                .withCachedFileHash(newFileHash)
                .withFetchErrorsCount(20L)
                .withOffersCount(1500L)
                .withOfferExamples(null);

        Feed actualFeed = feedRepository.get(clientInfo.getShard(), clientInfo.getClientId(), List.of(feedId)).get(0);
        assertThat(actualFeed).is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
    }

    private static FeedInfo getFeedInfo(ClientInfo clientInfo, Long marketShopId) {
        var mbiSynced = marketShopId != null ? StatusMBISynced.YES : StatusMBISynced.NO;
        return new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withFeedType(FeedType.YANDEX_MARKET)
                        .withBusinessType(BusinessType.RETAIL)
                        .withMarketShopId(marketShopId)
                        .withStatusMbiSynced(mbiSynced))
                .withClientInfo(clientInfo);
    }

}
