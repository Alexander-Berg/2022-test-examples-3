package ru.yandex.direct.logicprocessor.processors.bsexport.feeds;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.adv.direct.feeds.FeedAccess;
import ru.yandex.direct.bstransport.yt.repository.feeds.FeedsYtRepository;
import ru.yandex.direct.common.log.service.LogBsExportEssService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.dbschema.ppc.enums.FeedsBusinessType;
import ru.yandex.direct.ess.logicobjects.bsexport.feeds.BsExportFeedsObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BsExportFeedsServiceTest {

    private BsExportFeedsService bsExportFeedsService;
    private FeedRepository feedRepository;
    private FeedsYtRepository feedsYtRepository;
    private LogBsExportEssService logBsExportEssService;

    @BeforeEach
    void before() {
        feedRepository = mock(FeedRepository.class);
        feedsYtRepository = mock(FeedsYtRepository.class);
        logBsExportEssService = mock(LogBsExportEssService.class);
        bsExportFeedsService = new BsExportFeedsService(feedRepository, feedsYtRepository, logBsExportEssService);
    }

    @SuppressWarnings("unchecked")
    @Test
    void doneFeedTest() {
        var feedId = 123L;
        var bsExportFeedsObject = new BsExportFeedsObject(feedId);

        var feedFromDb = createDefaultFeedFromDb(feedId);
        when(feedRepository.get(anyInt(), anyCollection())).thenReturn(List.of(feedFromDb));
        bsExportFeedsService.processFeeds(1, List.of(bsExportFeedsObject));

        ArgumentCaptor<Collection> modifyFeedsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> modifyFeedsAccessCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(feedsYtRepository).modifyFeeds(modifyFeedsCaptor.capture());
        verify(feedsYtRepository).modifyFeedsAccess(modifyFeedsAccessCaptor.capture());
        verify(feedsYtRepository, never()).delete(anyCollection());

        var gotModifiedFeeds = (List<ru.yandex.adv.direct.feeds.Feed>)
                modifyFeedsCaptor.getValue();
        var gotModifiedFeedsAccess = (List<FeedAccess>) modifyFeedsAccessCaptor.getValue();

        var expectedFeeds = createDefaultExpectedFeed(feedId).build();

        var expectedFeedAccess = FeedAccess.newBuilder()
                .setFeedId(feedId)
                .setLogin("login")
                .setPassword("pass")
                .build();

        assertThat(gotModifiedFeeds).hasSize(1);
        assertThat(gotModifiedFeedsAccess).hasSize(1);

        assertThat(gotModifiedFeeds)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedFeeds);

        assertThat(gotModifiedFeedsAccess)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedFeedAccess);
    }

    @SuppressWarnings("unchecked")
    @Test
    void doneFeedWithNullLoginAndPasswordTest() {
        var feedId = 123L;
        var bsExportFeedsObject = new BsExportFeedsObject(feedId);

        var feedFromDb = createDefaultFeedFromDb(feedId);
        feedFromDb.withLogin(null).withPlainPassword(null);

        when(feedRepository.get(anyInt(), anyCollection())).thenReturn(List.of(feedFromDb));
        bsExportFeedsService.processFeeds(1, List.of(bsExportFeedsObject));

        ArgumentCaptor<Collection> modifyFeedsAccessCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(feedsYtRepository).modifyFeedsAccess(modifyFeedsAccessCaptor.capture());

        var gotModifiedFeedsAccess = (List<FeedAccess>) modifyFeedsAccessCaptor.getValue();


        assertThat(gotModifiedFeedsAccess).hasSize(1);

        var gotModifiedFeedAccess = gotModifiedFeedsAccess.get(0);
        assertThat(gotModifiedFeedAccess.getFeedId()).isEqualTo(feedId);
        assertThat(gotModifiedFeedAccess.getLogin()).isEqualTo("");
        assertThat(gotModifiedFeedAccess.getPassword()).isEqualTo("");
    }

    @Test
    void notDoneFeedTest() {
        var feedId = 123L;
        var bsExportFeedsObject = new BsExportFeedsObject(feedId);

        var feedFromDb = createDefaultFeedFromDb(feedId);
        feedFromDb.setUpdateStatus(UpdateStatus.UPDATING);
        when(feedRepository.get(anyInt(), anyCollection())).thenReturn(List.of(feedFromDb));
        bsExportFeedsService.processFeeds(1, List.of(bsExportFeedsObject));

        verify(feedsYtRepository, never()).modifyFeeds(anyCollection());
        verify(feedsYtRepository, never()).modifyFeedsAccess(anyCollection());
        verify(feedsYtRepository, never()).delete(anyCollection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletedFeedTest() {
        var feedId = 123L;
        var bsExportFeedsObject = new BsExportFeedsObject(feedId, true);

        var feedFromDb = createDefaultFeedFromDb(feedId);
        feedFromDb.setUpdateStatus(UpdateStatus.UPDATING);
        when(feedRepository.get(anyInt(), anyCollection())).thenReturn(List.of(feedFromDb));
        bsExportFeedsService.processFeeds(1, List.of(bsExportFeedsObject));

        ArgumentCaptor<Collection> feedIdsToDeleteCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(feedsYtRepository, never()).modifyFeeds(anyCollection());
        verify(feedsYtRepository, never()).modifyFeedsAccess(anyCollection());
        verify(feedsYtRepository).delete(feedIdsToDeleteCaptor.capture());

        var gotFeedIds = (List<Long>) feedIdsToDeleteCaptor.getValue();
        assertThat(gotFeedIds)
                .containsExactlyInAnyOrder(feedId);
    }

    private Feed createDefaultFeedFromDb(long feedId) {
        return new Feed()
                .withId(feedId)
                .withClientId(234L)
                .withBusinessType(BusinessType.RETAIL)
                .withUpdateStatus(UpdateStatus.DONE)
                .withLogin("login")
                .withPlainPassword("pass")
                .withUrl("https://yandex.ru")
                .withIsRemoveUtm(false)
                .withFeedType(FeedType.YANDEX_MARKET);
    }

    private ru.yandex.adv.direct.feeds.Feed.Builder createDefaultExpectedFeed(long feedId) {
        return ru.yandex.adv.direct.feeds.Feed.newBuilder()
                .setFeedId(feedId)
                .setClientId(234L)
                .setFeedType(FeedType.YANDEX_MARKET.getTypedValue())
                .setBusinessType(FeedsBusinessType.retail.getLiteral())
                .setUrl("https://yandex.ru")
                .setIsRemoteUtm(false);
    }
}
