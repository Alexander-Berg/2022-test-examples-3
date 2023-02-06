package ru.yandex.market.mboc.tms.executors.datacamp;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DatacampMigrationSyncExecutorTest extends BaseDbTestClass {

    private DatacampMigrationSyncExecutor datacampMigrationSyncExecutor;
    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private MigrationOfferRepository migrationOfferRepository;

    @Before
    public void setUp() throws Exception {
        datacampMigrationSyncExecutor = new DatacampMigrationSyncExecutor(
            namedParameterJdbcTemplate
        );
    }

    @Test
    public void willLoadMigrationOffersForSync() {
        int targetBusinessId = 123;
        String shopSkuOk = "TestShopSku";
        String shopSkuNotOk = "TestShopSku1";

        var migrationStatusOk = new MigrationStatus()
            .setTargetBusinessId(targetBusinessId)
            .setSourceBusinessId(111)
            .setSupplierId(1234)
            .setMigrationStatus(MigrationStatusType.RECEIVING)
            .setId(1L);

        var migrationStatusNotOk = new MigrationStatus()
            .setTargetBusinessId(targetBusinessId)
            .setSourceBusinessId(111)
            .setSupplierId(1234)
            .setMigrationStatus(MigrationStatusType.RECEIVING)
            .setId(2L);
        var savedMigrationStatusOk = migrationStatusRepository.save(migrationStatusOk);
        var savedMigrationStatusNotOk = migrationStatusRepository.save(migrationStatusNotOk);

        var migrationOfferOk = new MigrationOffer()
            .setId(savedMigrationStatusOk.getId())
            .setMigrationId(savedMigrationStatusOk.getId())
            .setState(MigrationOfferState.NEW)
            .setShopSku(shopSkuOk);

        var migrationOfferNotOk = new MigrationOffer()
            .setId(savedMigrationStatusNotOk.getId())
            .setMigrationId(savedMigrationStatusNotOk.getId())
            .setState(MigrationOfferState.PROCESSED)
            .setShopSku(shopSkuNotOk);

        migrationOfferRepository.save(migrationOfferOk, migrationOfferNotOk);

        datacampMigrationSyncExecutor.execute();

        var params = new MapSqlParameterSource();
        var datacampFailedImports = namedParameterJdbcTemplate.queryForList(
            "select * from mbo_category.datacamp_failed_import",
            params
        );

        assertNotNull(datacampFailedImports);
        assertEquals(1, datacampFailedImports.size());

        final Map<String, Object> failedImportMap = datacampFailedImports.get(0);
        assertEquals((long) targetBusinessId, failedImportMap.get("business_id"));
        assertEquals(shopSkuOk, failedImportMap.get("shop_sku"));
    }
}
