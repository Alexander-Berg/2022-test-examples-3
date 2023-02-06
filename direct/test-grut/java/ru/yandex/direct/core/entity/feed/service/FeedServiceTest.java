package ru.yandex.direct.core.entity.feed.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.configuration.GrutCoreTest;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.MasterSystem;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.StatusMBISynced;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.feed.validation.FeedDefectIds;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.uac.GrutSteps;
import ru.yandex.direct.market.client.MarketClient;
import ru.yandex.direct.market.client.exception.MarketClientException;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;

import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.createFakeFeedUrl;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.unFakeUrlIfNeeded;
import static ru.yandex.direct.core.testing.data.TestFeeds.DEFAULT_FEED_URL;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFileFeed;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.NO_RIGHTS;

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private GrutSteps grutSteps;

    @Autowired
    private FeedService feedService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private MarketClient marketClient;

    private ClientInfo clientInfo;
    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        grutSteps.createClient(clientInfo);
    }

    @Test
    public void updateFeeds_fileFeedUrlChanged() {
        var marketShopId = nextLong();
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFileFeed(null)
                        .withMarketShopId(marketShopId)
                        .withStatusMbiSynced(StatusMBISynced.YES)
                        .withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        modelChanges.process("filedata".getBytes(), Feed.FILE_DATA);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getMarketShopId()).isEqualTo(marketShopId);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isNotEqualTo(lastChange);
        softAssertions.assertThat(actualFeeds.get(0).getStatusMbiSynced()).isEqualTo(StatusMBISynced.NO);
        softAssertions.assertAll();
    }

    @Test
    public void updateFeeds_urlFeedUrlChanged() {
        var marketShopId = nextLong();
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withMarketShopId(marketShopId)
                        .withStatusMbiSynced(StatusMBISynced.YES)
                        .withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        var feedUrl = feed.getFeed().getUrl() + "?";
        modelChanges.process(feedUrl, Feed.URL);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getMarketShopId()).isEqualTo(marketShopId);
        softAssertions.assertThat(actualFeeds.get(0).getStatusMbiSynced()).isEqualTo(StatusMBISynced.NO);
        softAssertions.assertThat(actualFeeds.get(0).getUrl()).isEqualTo(feedUrl);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isNotEqualTo(lastChange);
        softAssertions.assertAll();
    }

    @Test
    public void updateFeeds_nonDirectUrlFeedUrlChanged() {
        var marketShopId = nextLong();
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withMasterSystem(MasterSystem.MARKET)
                        .withMarketShopId(marketShopId)
                        .withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        var feedUrl = feed.getFeed().getUrl() + "?";
        modelChanges.process(feedUrl, Feed.URL);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getMarketShopId()).isEqualTo(marketShopId);
        softAssertions.assertThat(actualFeeds.get(0).getUrl()).isEqualTo(feedUrl);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isNotEqualTo(lastChange);
        softAssertions.assertAll();
    }

    @Test
    public void updateFeeds_feedLoginChanged() {
        var marketShopId = nextLong();
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withMarketShopId(marketShopId)
                        .withStatusMbiSynced(StatusMBISynced.YES)
                        .withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        modelChanges.process(feed.getFeed().getLogin() + "1", Feed.LOGIN);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getMarketShopId()).isEqualTo(marketShopId);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isNotEqualTo(lastChange);
        softAssertions.assertThat(actualFeeds.get(0).getStatusMbiSynced()).isEqualTo(StatusMBISynced.NO);
        softAssertions.assertAll();
    }

    @Test
    public void updateFeeds_feedPasswordChanged() {
        var marketShopId = nextLong();
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withMarketShopId(marketShopId)
                        .withStatusMbiSynced(StatusMBISynced.YES)
                        .withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        modelChanges.process(feed.getFeed().getPlainPassword() + "1", Feed.PLAIN_PASSWORD);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getMarketShopId()).isEqualTo(marketShopId);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isNotEqualTo(lastChange);
        softAssertions.assertThat(actualFeeds.get(0).getStatusMbiSynced()).isEqualTo(StatusMBISynced.NO);
        softAssertions.assertAll();
    }

    @Test
    public void deleteFeeds_disableShops() {
        var feed = steps.feedSteps().createDefaultSyncedFeed(clientInfo);

        feedService.deleteFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(feed.getFeedId()));

        verify(marketClient, only()).sendUrlFeedToMbi(eq(new MarketClient.UrlFeedInfo(
                clientInfo.getClientId(),
                Objects.requireNonNull(clientInfo.getChiefUserInfo()).getUid(),
                feed.getFeedId(),
                unFakeUrlIfNeeded(feed.getFeed().getUrl()),
                feed.getFeed().getLogin(),
                feed.getFeed().getPlainPassword()
        )), eq(false));
    }

    @Test
    public void deleteFeeds_noFailureOnMarketClientException() {
        doThrow(new MarketClientException("Connection refused")).when(marketClient).sendUrlFeedToMbi(any(), any());

        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed()
                        .withMarketShopId(nextLong()))
                .withClientInfo(clientInfo));
        assertThatCode(() -> feedService.deleteFeeds(clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid(), List.of(feed.getFeedId())))
                .doesNotThrowAnyException();
        assertThat(feedRepository.get(shard, List.of(feed.getFeedId()))).isEmpty();
    }


    @Test
    public void updateFeeds_urlUrlFeedFakedUrlChanged() {
        var marketShopId = nextLong();
        var marketBusinessId = nextLong();
        var marketFeedId = nextLong();
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feedUrl = createFakeFeedUrl(marketBusinessId, marketShopId, marketFeedId, DEFAULT_FEED_URL, false, false);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withUrl(feedUrl)
                        .withSource(Source.URL)
                        .withUpdateStatus(UpdateStatus.DONE)
                        .withMarketShopId(marketShopId)
                        .withLastChange(lastChange)
                        .withStatusMbiSynced(StatusMBISynced.YES))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        modelChanges.process(DEFAULT_FEED_URL, Feed.URL);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getMarketShopId()).isEqualTo(marketShopId);
        softAssertions.assertThat(actualFeeds.get(0).getUpdateStatus()).isEqualTo(UpdateStatus.DONE);
        softAssertions.assertThat(actualFeeds.get(0).getUrl()).isEqualTo(feedUrl);
        softAssertions.assertThat(actualFeeds.get(0).getStatusMbiSynced()).isEqualTo(StatusMBISynced.YES);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isEqualTo(lastChange);
        softAssertions.assertAll();
    }

    @Test
    public void updateFeeds_noChangesSoLastChangeNotChanged() {
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null)
                        .withMarketShopId(nextLong())
                        .withLastChange(lastChange)
                        .withStatusMbiSynced(StatusMBISynced.YES))
                .withClientInfo(clientInfo));
        var modelChanges = new ModelChanges<>(feed.getFeedId(), Feed.class);
        var result = feedService.updateFeeds(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                List.of(modelChanges));
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getSuccessfulCount()).isEqualTo(1);
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getStatusMbiSynced()).isEqualTo(StatusMBISynced.YES);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isEqualTo(lastChange);
        softAssertions.assertAll();
    }

    @Test
    public void refreshFeed_success() {
        var lastChange = LocalDateTime.now().withNano(0).minusDays(1);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null).withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var result =
                feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                        feed.getFeedId());
        var actualFeeds = feedRepository.get(shard, List.of(feed.getFeedId()));

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        softAssertions.assertThat(actualFeeds).hasSize(1);
        softAssertions.assertThat(actualFeeds.get(0).getUpdateStatus()).isEqualTo(UpdateStatus.NEW);
        softAssertions.assertThat(actualFeeds.get(0).getLastChange()).isNotEqualTo(lastChange);
        softAssertions.assertAll();
    }

    @Test
    public void refreshFeed_withNoRightsError() {
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null).withLastChange(lastChange))
                .withClientInfo(clientInfo));

        ClientInfo badOperator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);
        var result =
                feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), badOperator.getUid(),
                        feed.getFeedId());

        assertThat(result.getValidationResult().flattenErrors())
                .is(matchedBy(contains(validationError(NO_RIGHTS))));
    }

    @Test
    public void refreshFeed_withRefreshHoursPeriodError() {
        var lastChange = LocalDateTime.now().minusMinutes(30).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null).withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var result =
                feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                        feed.getFeedId());

        assertThat(result.getValidationResult().flattenErrors())
                .is(matchedBy(contains(validationError(
                        FeedDefectIds.Number.FEED_CANNOT_BE_REFRESHED_MORE_OFTEN_THAN_REFRESH_HOURS_PERIOD
                ))));
    }

    @Test
    public void refreshFeed_twiceRefreshError() {
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null).withLastChange(lastChange).withUpdateStatus(UpdateStatus.DONE))
                .withClientInfo(clientInfo));
        feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(), feed.getFeedId());
        var result =
                feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                        feed.getFeedId());

        assertThat(result.getValidationResult().flattenErrors())
                .is(matchedBy(contains(validationError(FeedDefectIds.Gen.FEED_WITH_STATUS_NEW_CANNOT_BE_REFRESHED))));
    }

    @Test
    public void refreshFeed_withUpdateStatusNewError() {
        var lastChange = LocalDateTime.now().withNano(0).minusDays(1);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null).withLastChange(lastChange))
                .withClientInfo(clientInfo));
        feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(), feed.getFeedId());
        var result =
                feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                        feed.getFeedId());

        assertThat(result.getValidationResult().flattenErrors())
                .is(matchedBy(contains(validationError(FeedDefectIds.Gen.FEED_WITH_STATUS_NEW_CANNOT_BE_REFRESHED))));
    }

    @Test
    public void refreshFeed_withFeedNotExistError() {
        var lastChange = LocalDateTime.now().minusDays(1).withNano(0);
        var feed = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed(null).withLastChange(lastChange))
                .withClientInfo(clientInfo));
        var result =
                feedService.refreshFeed(clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                        feed.getFeedId() + 1);

        assertThat(result.getValidationResult().flattenErrors())
                .is(matchedBy(contains(validationError(AdGroupDefectIds.ModelId.FEED_NOT_EXIST))));
    }

    @Test
    public void getFeedIdsByCampaignIds_skipCampaignsWithoutFeeds() {
        var feed = steps.feedSteps().createDefaultFeed();
        var textGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        var dynamicTextGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        var dynamicFeedGroup = steps.adGroupSteps().createDynamicFeedAdGroup(clientInfo,
                activeDynamicFeedAdGroup(null, feed.getFeedId()));
        var smartGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feed.getFeedId());
        Map<Long, Set<Long>> expected = Map.of(
                dynamicFeedGroup.getCampaignId(), Set.of(feed.getFeedId()),
                smartGroup.getCampaignId(), Set.of(feed.getFeedId())
        );
        var actual = feedService.getFeedIdsByCampaignIds(shard, Set.of(textGroup.getCampaignId(),
                dynamicTextGroup.getCampaignId(), dynamicFeedGroup.getCampaignId(), smartGroup.getCampaignId()));

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    public void getFeedIdsByAdgroupsIds() {
        var feed = steps.feedSteps().createDefaultFeed();
        var textGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        var dynamicTextGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        var dynamicFeedGroup = steps.adGroupSteps().createDynamicFeedAdGroup(clientInfo,
                activeDynamicFeedAdGroup(null, feed.getFeedId()));
        var smartGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feed.getFeedId());
        Map<Long, Long> expected = Map.of(
                dynamicFeedGroup.getAdGroupId(), feed.getFeedId(),
                smartGroup.getAdGroupId(), feed.getFeedId()
        );
        var actual = feedService.getFeedIdsByAdGroupIds(shard, Set.of(textGroup.getAdGroupId(),
                dynamicTextGroup.getAdGroupId(), dynamicFeedGroup.getAdGroupId(), smartGroup.getAdGroupId()));

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    public void getFeedsSimple_nullFilterIds_emptyResult() {
        var actual = feedService.getFeedsSimple(shard, null);
        assertThat(actual).isEmpty();
    }

    @Test
    public void getFeedsSimple_emptyFilterIds_emptyResult() {
        var actual = feedService.getFeedsSimple(shard, Set.of());
        assertThat(actual).isEmpty();
    }

    @Test
    public void getFeedsSimple_filterById_returnFeed() {
        var feed = steps.feedSteps().createDefaultFeed();
        var actual = feedService.getFeedsSimple(shard, Set.of(feed.getFeedId()));
        assertThat(actual).isNotEmpty();
    }
}
