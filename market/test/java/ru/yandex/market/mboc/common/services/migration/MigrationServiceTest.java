package ru.yandex.market.mboc.common.services.migration;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.jooq.JSONB;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class MigrationServiceTest extends BaseDbTestClass {

    private static final Integer TARGET_BIZ = 1;
    private static final Integer SOURCE_BIZ = 2;
    private static final Integer SUPPLIER = 3;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    private OfferRepository offerRepository;

    private MigrationService migrationService;

    @Before
    public void setUp() {
        SupplierService supplierService = new SupplierService(supplierRepository);
        migrationService = new MigrationService(migrationStatusRepository, migrationOfferRepository,
            migrationRemovedOfferRepository, supplierRepository, offerUpdateSequenceService, offerMetaRepository);
    }

    @After
    public void tearDown() {
        migrationService.invalidateAll();
    }

    @Test
    public void testCacheUpdate() {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(44)
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        migrationService.checkAndUpdateCache();

        Set<Integer> inMigrationBusinesses = new HashSet<>();
        migrationService.acceptIfInMigration(List.of(44), inMigrationBusinesses::add);
        Assertions.assertThat(inMigrationBusinesses).containsExactly(44);

        migrationStatus = migrationStatusRepository.getById(1L);
        migrationStatus.setMigrationStatus(MigrationStatusType.FINISHED);
        migrationStatusRepository.save(migrationStatus);

        migrationService.checkAndUpdateCache();

        inMigrationBusinesses.clear();
        migrationService.acceptIfInMigration(List.of(44), inMigrationBusinesses::add);
        Assertions.assertThat(inMigrationBusinesses).isEmpty();
    }

    @Test
    public void testServiceCache() {
        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(44)
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        migrationService.checkAndUpdateCache();

        Set<Integer> suppliersAccepted = new HashSet<>();
        migrationService.acceptIfInMigration(List.of(1234123), suppliersAccepted::add);
        Assertions.assertThat(suppliersAccepted).containsExactly(1234123);

        int targetBiz = migrationService.getInMigrationTargetBusinessCached(1234123);
        Assert.assertEquals(44L, targetBiz);

        migrationStatus = migrationStatusRepository.getById(1L);
        migrationStatus.setMigrationStatus(MigrationStatusType.FINISHED);
        migrationStatusRepository.save(migrationStatus);

        migrationService.checkAndUpdateCache();

        suppliersAccepted.clear();
        migrationService.acceptIfInMigration(List.of(1234123), suppliersAccepted::add);
        Assertions.assertThat(suppliersAccepted).isEmpty();

        Integer nullTarget = migrationService.getInMigrationTargetBusinessCached(1234123);
        Assert.assertNull(nullTarget);
    }

    @Test
    public void testSerializeServiceOffer() {
        Offer.ServiceOffer serviceOffer = new Offer.ServiceOffer(123,
            MbocSupplierType.THIRD_PARTY,
            Offer.AcceptanceStatus.NEW);

        JSONB jsonb = migrationService.serializeServiceOffer(serviceOffer);
        var resultServiceOffer = migrationService.deserializeServiceOffer(jsonb);

        Assertions.assertThat(resultServiceOffer).isEqualToComparingFieldByField(serviceOffer);
    }

    public void initSuppliers() {
        Supplier targetBusiness = new Supplier(TARGET_BIZ,
            "target biz",
            "biz.biz",
            "biz org",
            MbocSupplierType.BUSINESS);
        Supplier sourceBusiness = new Supplier(SOURCE_BIZ,
            "source biz",
            "biz.biz",
            "biz org",
            MbocSupplierType.BUSINESS);
        Supplier supplier = new Supplier(SUPPLIER,
            "sup",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setBusinessId(SOURCE_BIZ);
        supplierRepository.insertBatch(targetBusiness, sourceBusiness, supplier);
    }
}
