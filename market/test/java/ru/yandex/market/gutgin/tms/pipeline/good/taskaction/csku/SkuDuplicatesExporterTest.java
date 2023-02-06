package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.ScheduledTaskService;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.SkuDuplicatesDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.records.SkuDuplicatesRecord;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.db.jooq.tables.SkuDuplicates.SKU_DUPLICATES;

public class SkuDuplicatesExporterTest extends BaseDbCommonTest {

    private static final long CATEGORY_ID_1 = 1L;
    private static final long CATEGORY_ID_2 = 2L;

    private SkuDuplicatesExporter skuDuplicatesExporter;
    SkuDuplicatesDao skuDuplicatesDao;
    @Before
    public void setUp() {
        SkuDuplicateService skuDuplicateService = mock(SkuDuplicateService.class);
        when(skuDuplicateService.writeToYt(argThat(
                rows ->
                    rows != null && rows.stream().allMatch(row -> row.getCategoryId() == CATEGORY_ID_1))))
                .thenReturn(true);
        when(skuDuplicateService.writeToYt(argThat(
                rows ->
                        rows != null && rows.stream().allMatch(row -> row.getCategoryId() == CATEGORY_ID_2))))
                .thenReturn(false);

        skuDuplicatesDao = new SkuDuplicatesDao(configuration);
        skuDuplicatesExporter = new SkuDuplicatesExporter(0, mock(ScheduledTaskService.class), 0,
                skuDuplicatesDao, skuDuplicateService);
    }

    @Test
    public void testSkuDuplicatesExporterHappyPath() {
        prepareData(true);
        skuDuplicatesExporter.runAction();
        int count = dsl().fetchCount(SKU_DUPLICATES);
        assertThat(count).isZero();
    }

    @Test
    public void testYtWriteFailed() {
        prepareData(false);
        assertThat(skuDuplicatesDao.count()).isEqualTo(3);
        skuDuplicatesExporter.runAction();
        int count = dsl().fetchCount(SKU_DUPLICATES);
        assertThat(count).isEqualTo(3);
    }

    private void prepareData(boolean isGoodForYtData) {
        long categoryId = isGoodForYtData ? CATEGORY_ID_1 : CATEGORY_ID_2;
        dsl().batchInsert(List.of(
                new SkuDuplicatesRecord(0L, 0L, 1L, categoryId, new Timestamp(System.currentTimeMillis())),
                new SkuDuplicatesRecord(1L, 1L, 2L, categoryId, new Timestamp(System.currentTimeMillis())),
                new SkuDuplicatesRecord(2L, 2L, 0L, categoryId, new Timestamp(System.currentTimeMillis()))))
                .execute();
    }
}
