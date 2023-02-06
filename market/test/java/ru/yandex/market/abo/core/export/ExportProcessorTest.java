package ru.yandex.market.abo.core.export;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.export.rating.IndexerShopRatingContentProvider;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         06.07.15
 */
public class ExportProcessorTest extends EmptyTest {
    @Autowired
    private PremodStatsLastDayProcessor premodStatsLastDayProcessor;
    @Autowired
    private IndexerShopRatingContentProvider indexerShopRatingContentProvider;
    @Autowired
    private ContentProvider classifierShopRatingContentProvider;

    @Autowired
    private NamedHistoryMdsS3Client historyMdsS3Client;
    @Autowired
    private PureHistoryMdsS3Client pureHistoryMdsS3Client;
    @Autowired
    private ResourceConfigurationProvider resourceConfigurationProvider;

    @Test
    @Disabled
    public void testAll() {
        pureHistoryMdsS3Client.deleteOld(resourceConfigurationProvider.getByName("shop-rating"));
        historyMdsS3Client.upload("shop-rating", indexerShopRatingContentProvider);
        historyMdsS3Client.upload("shop-premod-stat", premodStatsLastDayProcessor);
    }

    @Test
    @Disabled("Fix me after moving rating to pg!")
    public void testShopRatingContentProvider() {
        historyMdsS3Client.upload("shop-rating-classifier", classifierShopRatingContentProvider);
    }
}
