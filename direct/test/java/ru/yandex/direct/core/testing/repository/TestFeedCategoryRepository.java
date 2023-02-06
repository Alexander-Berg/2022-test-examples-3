package ru.yandex.direct.core.testing.repository;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.feed.model.FeedCategory;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.PERF_FEED_CATEGORIES;

public class TestFeedCategoryRepository {

    private final DslContextProvider dslContextProvider;

    public TestFeedCategoryRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void add(int shard, FeedCategory category) {
        Long id =
                UtilRepository.getNextId(dslContextProvider.ppc(shard), PERF_FEED_CATEGORIES, PERF_FEED_CATEGORIES.ID);
        dslContextProvider.ppc(shard)
                .insertInto(PERF_FEED_CATEGORIES)
                .set(PERF_FEED_CATEGORIES.ID, id)
                .set(PERF_FEED_CATEGORIES.CATEGORY_ID, RepositoryUtils.bigIntegerToULong(category.getCategoryId()))
                .set(PERF_FEED_CATEGORIES.PARENT_CATEGORY_ID,
                        RepositoryUtils.bigIntegerToULong(category.getParentCategoryId()))
                .set(PERF_FEED_CATEGORIES.FEED_ID, category.getFeedId())
                .set(PERF_FEED_CATEGORIES.NAME, category.getName())
                .set(PERF_FEED_CATEGORIES.OFFERS_COUNT, category.getOfferCount())
                .set(PERF_FEED_CATEGORIES.IS_DELETED, RepositoryUtils.booleanToLong(category.getIsDeleted()))
                .execute();
    }
}
