package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

/**
 * Создание тестовых объектов {@link BsExportQueueInfo}
 */
public class TestBsExportQueueRecords {
    public static final String FULL_EXPORT_SEQ_TIME_FIELD = "fullExportSequenceTime";
    public static final String[] TIME_FIELDS = {"queueTime", "sequenceTime", FULL_EXPORT_SEQ_TIME_FIELD};
    public static final String LOCKED_BY_FIELD = "lockedBy";

    public static BsExportQueueInfo recordWithFullStat(Long campaignId) {
        return new BsExportQueueInfo()
                .withCampaignId(campaignId)
                .withLockedBy(tinyIntRandom())
                .withSynchronizeValue(intRandom())
                .withCampaignsCount(1L)
                .withContextsCount(intRandom())
                .withBannersCount(intRandom())
                .withKeywordsCount(intRandom())
                .withPricesCount(intRandom())
                .withQueueTime(now())
                .withSequenceTime(now())
                .withNeedFullExport(true)
                .withFullExportSequenceTime(now());
    }

    public static BsExportQueueInfo recordWithoutStat(Long campaignId) {
        return new BsExportQueueInfo()
                .withCampaignId(campaignId)
                .withLockedBy(null)
                .withSynchronizeValue(0L)
                .withCampaignsCount(0L)
                .withContextsCount(0L)
                .withBannersCount(0L)
                .withKeywordsCount(0L)
                .withPricesCount(0L)
                .withNeedFullExport(false);
    }

    public static BsExportQueueInfo yesterdayRecordWithoutStat(Long campaignId) {
        LocalDateTime yesterday = now().minusDays(1);
        return recordWithoutStat(campaignId)
                .withQueueTime(yesterday)
                .withSequenceTime(yesterday)
                .withFullExportSequenceTime(yesterday);
    }

    private static Long intRandom() {
        return RandomUtils.nextLong(1, 30_000);
    }

    private static Integer tinyIntRandom() {
        return RandomUtils.nextInt(1, 125);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
