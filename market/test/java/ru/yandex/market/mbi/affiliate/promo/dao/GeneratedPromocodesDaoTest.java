package ru.yandex.market.mbi.affiliate.promo.dao;

import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.model.GeneratedPromoUploadStatus;
import ru.yandex.market.mbi.affiliate.promo.model.GeneratedPromocode;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class GeneratedPromocodesDaoTest {
    @Autowired
    private GeneratedPromocodesDao dao;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "generated_promocodes_dao.before.csv",
            after = "generated_promocodes_dao.insert.after.csv")
    public void testInsert() {
        dao.insert(List.of("MMMM-AG-AF", "MMMN-AG-AF"), 12345L, "UPLOAD_ABAB", 10001);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "generated_promocodes_dao.before.csv",
            after = "generated_promocodes_dao.before.csv")
    public void testInsertEmpty() {
        dao.insert(List.of(), 12345L, "UPLOAD_ABAB", 10001);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "generated_promocodes_dao.before.csv",
            after = "generated_promocodes_dao.updateStatus.after.csv")
    public void testUpdateStatus() {
        dao.updateStatus(List.of("BBBC-AG-AF", "CCCC-AG-AF"), GeneratedPromoUploadStatus.FINISHED);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "generated_promocodes_dao.before.csv",
            after = "generated_promocodes_dao.before.csv")
    public void testUpdateStatusEmpty() {
        dao.updateStatus(List.of(), GeneratedPromoUploadStatus.FINISHED);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testGetBatchNotStartedExistUnderLimit() {
        var result = dao.getBatchNotStarted(10);
        assertThat(result).containsExactlyInAnyOrder(
                new GeneratedPromocode("CCCC-AG-AF", 10000, 11111, "UPLOAD_BBBB"),
                new GeneratedPromocode("CCCA-AG-AF", 10000, 11111, "UPLOAD_BBBB"),
                new GeneratedPromocode("DDDD-AG-AF", 10000, 12345, "UPLOAD_DDDD")
        );
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testGetBatchNotStartedExistOverLimit() {
        var result = dao.getBatchNotStarted(2);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.emptyBatch.before.csv")
    public void testGetBatchNotStartedEmpty() {
        var result = dao.getBatchNotStarted(3);
        assertThat(result).isEmpty();
    }


    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testGetByUploadId() {
        var result = dao.getValues("UPLOAD_AACC");
        assertThat(result).containsExactlyInAnyOrder("BBBB-AG-AF", "BBBC-AG-AF");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testGetByUploadIdEmpty() {
        var result = dao.getValues("UPLOAD_GGGG");
        assertThat(result).isEmpty();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testIsAllPromosFinishedUploadTrue() {
        var result = dao.isAllPromosFinishedUpload("UPLOAD_AAAA");
        assertThat(result).isTrue();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testIsAllPromosFinishedUploadFalse() {
        var result = dao.isAllPromosFinishedUpload("UPLOAD_AACC");
        assertThat(result).isFalse();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "generated_promocodes_dao.before.csv")
    public void testIsAllPromosFinishedUploadNoData() {
        var result = dao.isAllPromosFinishedUpload("UPLOAD_GGGG");
        assertThat(result).isTrue();
    }
}