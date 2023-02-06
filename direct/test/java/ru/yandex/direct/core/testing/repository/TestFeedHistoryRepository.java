package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItem;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.PERF_FEED_HISTORY;

public class TestFeedHistoryRepository {
    private final DslContextProvider dslContextProvider;

    public TestFeedHistoryRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void add(int shard, FeedHistoryItem historyItem) {
        Long id = UtilRepository.getNextId(dslContextProvider.ppc(shard), PERF_FEED_HISTORY, PERF_FEED_HISTORY.ID);
        LocalDateTime createdAt = historyItem.getCreatedAt() != null ? historyItem.getCreatedAt() : LocalDateTime.now();
        dslContextProvider.ppc(shard)
                .insertInto(PERF_FEED_HISTORY)
                .set(PERF_FEED_HISTORY.ID, id)
                .set(PERF_FEED_HISTORY.FEED_ID, historyItem.getFeedId())
                .set(PERF_FEED_HISTORY.CREATED_AT, createdAt)
                .set(PERF_FEED_HISTORY.OFFERS_COUNT, historyItem.getOfferCount())
                .set(PERF_FEED_HISTORY.PARSE_RESULTS_JSON_COMPRESSED,
                        RepositoryUtils.toCompressedJsonMediumblobDb(historyItem.getParseResults()))
                .execute();
    }
}
