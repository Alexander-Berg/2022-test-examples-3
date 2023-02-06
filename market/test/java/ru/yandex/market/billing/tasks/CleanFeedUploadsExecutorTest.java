package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.FeedService;

class CleanFeedUploadsExecutorTest extends FunctionalTest {

    @Autowired
    private FeedService feedService;

    private CleanFeedUploadsExecutor cleanFeedUploadsExecutor;

    @BeforeEach
    void initExecutor() {
        cleanFeedUploadsExecutor = new CleanFeedUploadsExecutor(feedService);
    }

    /**
     * Все аплоады старые и не привязаны к фидам - все должны быть  удалены
     */
    @Test
    @DbUnitDataSet(
            before = "CleanFeedUploadsAllOrphanedAndExpired.before.csv",
            after = "CleanFeedUploadsAllOrphanedAndExpired.after.csv"
    )
    void uploadFeedsAllOrphanedAndExpired() {
        cleanFeedUploadsExecutor.doJob(null);
    }

    /**
     * Все аплоады старые, но привязаны к фидам - все должны остаться
     */
    @Test
    @DbUnitDataSet(
            before = "CleanFeedUploadAllBoundAndExpired.before.csv",
            after = "CleanFeedUploadAllBoundAndExpired.after.csv"
    )
    void uploadFeedsAllBoundAndExpired() {
        cleanFeedUploadsExecutor.doJob(null);
    }


    /**
     * Все аплоады не привязаны к фидам, но не устарели - все должны остаться
     */
    @Test
    @DbUnitDataSet(
            before = "CleanFeedUploadAllOrphanedAndNonExpired.before.csv",
            after = "CleanFeedUploadAllOrphanedAndNonExpired.after.csv"
    )
    void uploadFeedsAllOrphanedAndNonExpired() {
        cleanFeedUploadsExecutor.doJob(null);
    }
}