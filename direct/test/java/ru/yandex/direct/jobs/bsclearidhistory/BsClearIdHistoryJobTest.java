package ru.yandex.direct.jobs.bsclearidhistory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.jooq.types.ULong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.keyword.container.CampaignIdAndKeywordIdPair;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.statistics.container.AdGroupIdAndPhraseIdPair;
import ru.yandex.direct.core.entity.statistics.repository.BsAuctionStatRepository;
import ru.yandex.direct.utils.TimeProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на джобу {@link ru.yandex.direct.jobs.bsclearidhistory.BsClearIdHistoryJob}
 */
class BsClearIdHistoryJobTest {

    private static final int SHARD = 2;
    private static final int STAT_TIME_EXPIRE_DAYS = 1;
    private static final int DELETE_FROM_PRIORITIES_LIMIT = 500_000;
    private static final int BS_HISTORY_EXPIRE_HOURS = 6;
    private static final int BS_HISTORY_ARC_EXPIRE_DAYS = 90;
    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.of(2018, 9, 1, 12, 0);

    private static LocalDateTime statTimeLimit;
    private static LocalDateTime historyArcExpireDateTime;
    private static LocalDateTime historyExpireDateTime;

    private BsClearIdHistoryJob bsClearIdHistoryJob;
    private BsAuctionStatRepository bsAuctionStatRepository;
    private KeywordRepository keywordRepository;
    private TimeProvider timeProvider;

    @BeforeEach
    void initMocks() {
        bsAuctionStatRepository = mock(BsAuctionStatRepository.class);
        keywordRepository = mock(KeywordRepository.class);
        timeProvider = mock(TimeProvider.class);
        bsClearIdHistoryJob = new BsClearIdHistoryJob(SHARD, bsAuctionStatRepository, keywordRepository, timeProvider);
        statTimeLimit = CURRENT_DATE_TIME.minusDays(STAT_TIME_EXPIRE_DAYS);
        historyArcExpireDateTime = CURRENT_DATE_TIME.minusDays(BS_HISTORY_ARC_EXPIRE_DAYS);
        historyExpireDateTime = CURRENT_DATE_TIME.minusHours(BS_HISTORY_EXPIRE_HOURS);
        when(timeProvider.now()).thenReturn(CURRENT_DATE_TIME);

    }

    /**
     * Тестируем случай, когда нет подходящих для удаления данных.
     * Сервис должен передать в методы удаления пустые списки
     */
    @Test
    void execute_NoDataToDelete_NoDelete() {
        when(bsAuctionStatRepository.getUnusedIds(eq(SHARD), eq(statTimeLimit), eq(DELETE_FROM_PRIORITIES_LIMIT)))
                .thenReturn(Collections.emptyList());
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForDeletedCampaigns(eq(SHARD))).thenReturn(Collections.emptyList());
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForArchivedCampaigns(eq(SHARD), eq(historyArcExpireDateTime)))
                .thenReturn(Collections.emptyList());
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForNotArchivedCampaigns(eq(SHARD), eq(historyExpireDateTime)))
                .thenReturn(Collections.emptyList());

        bsClearIdHistoryJob.execute();

        verify(bsAuctionStatRepository, never()).deleteUnusedByIds(eq(SHARD), anyList(),
                any(LocalDateTime.class));
        verify(keywordRepository, never()).deleteFromBidsPhraseIdHistoryTable(eq(SHARD), anyList());
    }

    /**
     * Тестируем удаление из bids_phraseid_history
     * Сервис должен вызвать метод удаления с ожидаемым набором ключей
     */
    @Test
    void execute_BidsPhrases_DeleteCalled() {
        when(bsAuctionStatRepository.getUnusedIds(eq(SHARD), eq(statTimeLimit), eq(DELETE_FROM_PRIORITIES_LIMIT)))
                .thenReturn(Collections.emptyList());

        List<CampaignIdAndKeywordIdPair> deletedBidsPhrases = List.of(new CampaignIdAndKeywordIdPair(1L, 1L));
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForDeletedCampaigns(eq(SHARD))).thenReturn(deletedBidsPhrases);

        List<CampaignIdAndKeywordIdPair> archived = List.of(new CampaignIdAndKeywordIdPair(2L, 2L));
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForArchivedCampaigns(eq(SHARD), eq(historyArcExpireDateTime)))
                .thenReturn(archived);

        List<CampaignIdAndKeywordIdPair> notArchived = List.of(new CampaignIdAndKeywordIdPair(3L, 3L));
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForNotArchivedCampaigns(eq(SHARD), eq(historyExpireDateTime)))
                .thenReturn(notArchived);

        bsClearIdHistoryJob.execute();

        List<CampaignIdAndKeywordIdPair> expected = List.of(
                new CampaignIdAndKeywordIdPair(1L, 1L),
                new CampaignIdAndKeywordIdPair(2L, 2L),
                new CampaignIdAndKeywordIdPair(3L, 3L));

        verify(keywordRepository).deleteFromBidsPhraseIdHistoryTable(eq(SHARD), eq(expected));
    }

    /**
     * Тестируем удаление из bs_auction_stat
     * Сервис должен вызвать метод удаления с ожидаемым набором ключей
     */
    @Test
    void execute_unusedIds_unusedIdsDeleted() {
        List<AdGroupIdAndPhraseIdPair> unusedIds = List.of(
                new AdGroupIdAndPhraseIdPair(1, ULong.valueOf(1)),
                new AdGroupIdAndPhraseIdPair(2, ULong.valueOf(2)));

        when(bsAuctionStatRepository.getUnusedIds(eq(SHARD), eq(statTimeLimit), eq(DELETE_FROM_PRIORITIES_LIMIT)))
                .thenReturn(unusedIds);
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForDeletedCampaigns(eq(SHARD))).thenReturn(Collections.emptyList());
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForArchivedCampaigns(eq(SHARD), eq(historyArcExpireDateTime)))
                .thenReturn(Collections.emptyList());
        when(keywordRepository.getIdsOfBidsPhraseIdHistoryForNotArchivedCampaigns(eq(SHARD), eq(historyExpireDateTime)))
                .thenReturn(Collections.emptyList());

        bsClearIdHistoryJob.execute();

        List<AdGroupIdAndPhraseIdPair> expected = List.of(
                new AdGroupIdAndPhraseIdPair(1, ULong.valueOf(1)),
                new AdGroupIdAndPhraseIdPair(2, ULong.valueOf(2)));

        verify(bsAuctionStatRepository).deleteUnusedByIds(eq(SHARD), eq(expected),
                eq(CURRENT_DATE_TIME.minusDays(STAT_TIME_EXPIRE_DAYS)));
    }
}
