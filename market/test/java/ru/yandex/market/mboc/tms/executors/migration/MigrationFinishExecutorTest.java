package ru.yandex.market.mboc.tms.executors.migration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class MigrationFinishExecutorTest extends BaseDbTestClass {
    private static final Integer DST_BIZ = 1;
    private static final Integer SRC_BIZ = 2;
    private static final Integer BLUE_SHOP_ID = 3;
    private static final Integer WHITE_SHOP_ID = 4;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private MigrationOfferRepository migrationOfferRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;

    private MigrationService migrationService;
    private MigrationFinishExecutor migrationFinishExecutor;

    @Before
    public void setUp() throws Exception {
        SupplierService supplierService = new SupplierService(supplierRepository);

        migrationService = new MigrationService(migrationStatusRepository,
            migrationOfferRepository, migrationRemovedOfferRepository,
                supplierRepository, offerUpdateSequenceService, offerMetaRepository);
        migrationFinishExecutor = new MigrationFinishExecutor(migrationService,
            migrationStatusRepository,
            Mockito.mock(MonitoringUnit.class));

        Supplier targetBusiness = new Supplier(DST_BIZ,
            "target biz",
            "biz.biz",
            "biz org",
            MbocSupplierType.BUSINESS);
        Supplier sourceBusiness = new Supplier(SRC_BIZ,
            "source biz",
            "biz.biz",
            "biz org",
            MbocSupplierType.BUSINESS);
        Supplier supplier = new Supplier(BLUE_SHOP_ID,
            "sup",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setBusinessId(SRC_BIZ);
        Supplier whiteSupplier = new Supplier(WHITE_SHOP_ID,
            "sup",
            "sup.biz",
            "biz org",
            MbocSupplierType.MARKET_SHOP)
            .setBusinessId(SRC_BIZ);
        supplierRepository.insertBatch(targetBusiness, sourceBusiness, supplier, whiteSupplier);
    }

    @After
    public void tearDown() {
        migrationService.invalidateAll();
    }

    @Test
    public void testMigrationDoNotTouchIsActive() {
        var migrationStatus = startMigration(DST_BIZ, SRC_BIZ, BLUE_SHOP_ID, MigrationStatusType.ACTIVE);

        MigrationOffer migrationOffer = new MigrationOffer()
            .setMigrationId(migrationStatus.getId())
            .setShopSku("shopSku")
            .setState(MigrationOfferState.RECEIVED);
        migrationOfferRepository.save(migrationOffer);

        migrationService.checkAndUpdateCache();
        migrationFinishExecutor.execute();

        var newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        Assert.assertEquals(MigrationStatusType.ACTIVE, newMigrationStatus.getMigrationStatus());
    }

    @Test
    public void testMigrationIsFinished() {
        var migrationStatus = startMigration(DST_BIZ, SRC_BIZ, BLUE_SHOP_ID, MigrationStatusType.RECEIVING);

        MigrationOffer migrationOffer = new MigrationOffer()
            .setMigrationId(migrationStatus.getId())
            .setShopSku("shopSku")
            .setState(MigrationOfferState.NEW);
        migrationOffer = migrationOfferRepository.save(migrationOffer);

        migrationService.checkAndUpdateCache();
        migrationFinishExecutor.execute();

        var newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        Assert.assertEquals(MigrationStatusType.RECEIVING, newMigrationStatus.getMigrationStatus());

        migrationOffer.setState(MigrationOfferState.RECEIVED);
        migrationOfferRepository.save(migrationOffer);

        migrationService.checkAndUpdateCache();
        migrationFinishExecutor.execute();

        newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        Assert.assertEquals(MigrationStatusType.RECEIVED, newMigrationStatus.getMigrationStatus());
    }

    @Test
    public void testMigrationError() {
        var failedMigration = startMigration(100001, 100002, 100003, MigrationStatusType.RECEIVING);
        var migrationStatus = startMigration(DST_BIZ, SRC_BIZ, BLUE_SHOP_ID, MigrationStatusType.RECEIVED);

        MigrationOffer migrationOffer = new MigrationOffer()
            .setMigrationId(migrationStatus.getId())
            .setShopSku("shopSku")
            .setState(MigrationOfferState.RECEIVED);
        migrationOfferRepository.save(migrationOffer);

        MigrationOffer failedMigrationOffer = new MigrationOffer()
            .setMigrationId(failedMigration.getId())
            .setShopSku("shopSku1")
            .setState(MigrationOfferState.ERROR);
        migrationOfferRepository.save(failedMigrationOffer);

        migrationService.checkAndUpdateCache();
        migrationFinishExecutor.execute();

        var newMigrationStatus = migrationStatusRepository.getById(migrationStatus.getId());
        Assert.assertEquals(MigrationStatusType.RECEIVED, newMigrationStatus.getMigrationStatus());
        var newFailedMigration = migrationStatusRepository.getById(failedMigration.getId());
        Assert.assertEquals(MigrationStatusType.RECEIVING, newFailedMigration.getMigrationStatus());
    }

    private MigrationStatus startMigration(int target, int source, int supplier, MigrationStatusType status) {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setTargetBusinessId(target)
            .setSourceBusinessId(source)
            .setSupplierId(supplier)
            .setMigrationStatus(status);
        return migrationStatusRepository.save(migrationStatus);
    }
}
