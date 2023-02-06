package ru.yandex.market.mboc.tms.executors;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.RemovedOffer;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoveOffersExecutorTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private RemovedOfferRepository removedOfferRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private MigrationOfferRepository migrationOfferRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    private OfferUpdateSequenceService offerUpdateSequenceService;

    private MigrationService migrationService;

    private RemoveOffersExecutor removeOffersExecutor;

    @Before
    public void setUp() {
        offerUpdateSequenceService = Mockito.mock(OfferUpdateSequenceService.class);
        migrationService = Mockito.spy(new MigrationService(
            migrationStatusRepository,
            migrationOfferRepository,
            migrationRemovedOfferRepository,
            supplierRepository,
            offerUpdateSequenceService,
            offerMetaRepository
        ));
        removeOffersExecutor = new RemoveOffersExecutor(
            offerRepository,
            removedOfferRepository,
            transactionHelper,
            storageKeyValueService,
            migrationService
        );
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testRemoveOffers() throws Exception {
        offerRepository.insertOffers(List.of(
            OfferTestUtils.simpleOffer(1),
            OfferTestUtils.simpleOffer(2),
            OfferTestUtils.simpleOffer(3),
            OfferTestUtils.simpleOffer(4)
        ));

        var now = Instant.now();
        removedOfferRepository.save(List.of(
            new RemovedOffer(1L, now.minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).minusSeconds(10),
                false, null, null),
            new RemovedOffer(2L, now.minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).plusSeconds(10),
                false, null, null)
        ));

        removeOffersExecutor.doRealJob(null);

        var offers = offerRepository.findAll();
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(2L, 3L, 4L);

        var removedOffers = removedOfferRepository.findAll().stream()
            .collect(Collectors.partitioningBy(RemovedOffer::getIsRemoved));
        Assertions.assertThat(removedOffers.get(true)).extracting(RemovedOffer::getId).containsExactly(1L);
        Assertions.assertThat(removedOffers.get(true)).extracting(RemovedOffer::getRemovedOffer).isNotEmpty();
        Assertions.assertThat(removedOffers.get(true)).extracting(RemovedOffer::getRemovedOfferContent).isNotEmpty();
        Assertions.assertThat(removedOffers.get(false)).extracting(RemovedOffer::getId).containsExactly(2L);
        Assertions.assertThat(removedOffers.get(false)).extracting(RemovedOffer::getRemovedOffer).containsNull();
        Assertions.assertThat(removedOffers.get(true)).extracting(RemovedOffer::getRemovedOfferContent).isNotEmpty();
    }

    @Test
    public void shouldTransferEditRightsToFreshestTargetOffer() throws Exception {
        final Supplier savedSupplier1 = supplierRepository.insert(
            OfferTestUtils.simpleSupplier()
                .setId(43)
        );
        final Supplier savedSupplier2 = supplierRepository.insert(
            OfferTestUtils.simpleSupplier()
                .setId(44)
        );

        final Offer sourceSelfOffer1 = OfferTestUtils.simpleOffer(1)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(OfferTestUtils.mapping(1))
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);

        final Offer targetOffer1 = OfferTestUtils.simpleOffer(11)
            .setBusinessId(savedSupplier1.getId())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setServiceOffers(new Offer.ServiceOffer().setSupplierId(1))
            .setApprovedSkuMappingInternal(OfferTestUtils.mapping(1))
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.MIGRATED);
        final Offer targetOldOffer2 = OfferTestUtils.simpleOffer(12)
            .setBusinessId(savedSupplier2.getId())
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(OfferTestUtils.mapping(2))
            .setShopSku("TestShopSku2")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.MIGRATED);

        offerRepository.insertOffers(sourceSelfOffer1, targetOffer1, targetOldOffer2);

        final MigrationStatus migrationStatus1 = new MigrationStatus()
            .setId(1L)
            .setSourceBusinessId(sourceSelfOffer1.getBusinessId())
            .setTargetBusinessId(targetOffer1.getBusinessId())
            .setSupplierId(111)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.FINISHED);

        final MigrationStatus oldMigrationStatus2 = new MigrationStatus()
            .setId(2L)
            .setSourceBusinessId(sourceSelfOffer1.getBusinessId())
            .setTargetBusinessId(targetOldOffer2.getBusinessId())
            .setSupplierId(111)
            .setCreatedTs(Instant.now().minusSeconds(1))
            .setMigrationStatus(MigrationStatusType.FINISHED);

        migrationStatusRepository.save(migrationStatus1, oldMigrationStatus2);

        final MigrationOffer migrationOffer1 = new MigrationOffer()
            .setMigrationId(migrationStatus1.getId())
            .setShopSku(sourceSelfOffer1.getShopSku())
            .setState(MigrationOfferState.PROCESSED);

        migrationOfferRepository.save(migrationOffer1);

        removedOfferRepository.save(List.of(
            new RemovedOffer(
                sourceSelfOffer1.getId(),
                Instant.now().minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).minusSeconds(10),
                false, null, null)
        ));

        removeOffersExecutor.doRealJob(null);

        final Offer resultTargetOffer = offerRepository.findOfferByBusinessSkuKey(targetOffer1.getBusinessSkuKey());
        final Offer sourceOffer = offerRepository.findOfferByBusinessSkuKey(sourceSelfOffer1.getBusinessSkuKey());

        assertNull(sourceOffer);
        assertNotNull(resultTargetOffer);
        assertEquals(targetOffer1.getId(), resultTargetOffer.getId());
        assertEquals(Offer.MappingConfidence.PARTNER_SELF, resultTargetOffer.getApprovedSkuMappingConfidence());
        assertEquals(targetOffer1.getApprovedSkuMapping().getMappingId(), resultTargetOffer.getApprovedSkuMapping().getMappingId());
    }

    @Test
    public void shouldTransferOfferRightsAllOfRemovingOfferOfOneBusiness() throws Exception {
        final Supplier targetSupplier = supplierRepository.insert(
            OfferTestUtils.simpleSupplier()
                .setId(43)
        );

        final Offer sourceSelfOffer1 = OfferTestUtils.simpleOffer(1)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);
        final Offer sourceSelfOffer2 = OfferTestUtils.simpleOffer(2)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("TestShopSku2")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);

        final Offer targetOffer1 = OfferTestUtils.simpleOffer(11)
            .setBusinessId(targetSupplier.getId())
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.MIGRATED);
        final Offer targetOffer2 = OfferTestUtils.simpleOffer(12)
            .setBusinessId(targetSupplier.getId())
            .setShopSku("TestShopSku2")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.MIGRATED);
        final Offer targetOffer3 = OfferTestUtils.simpleOffer(13)
            .setBusinessId(targetSupplier.getId())
            .setShopSku("TestShopSku3")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.MIGRATED);

        offerRepository.insertOffers(sourceSelfOffer1, sourceSelfOffer2, targetOffer1, targetOffer2, targetOffer3);

        final MigrationStatus migrationStatus1 = new MigrationStatus()
            .setId(1L)
            .setSourceBusinessId(sourceSelfOffer1.getBusinessId())
            .setTargetBusinessId(targetOffer1.getBusinessId())
            .setSupplierId(111)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.FINISHED);

        migrationStatusRepository.save(migrationStatus1);

        final MigrationOffer migrationOffer1 = new MigrationOffer()
            .setMigrationId(migrationStatus1.getId())
            .setShopSku(sourceSelfOffer1.getShopSku())
            .setState(MigrationOfferState.PROCESSED);
        final MigrationOffer migrationOffer2 = new MigrationOffer()
            .setMigrationId(migrationStatus1.getId())
            .setShopSku(targetOffer2.getShopSku())
            .setState(MigrationOfferState.PROCESSED);
        final MigrationOffer migrationOffer3 = new MigrationOffer()
            .setMigrationId(migrationStatus1.getId())
            .setShopSku(targetOffer3.getShopSku())
            .setState(MigrationOfferState.PROCESSED);

        migrationOfferRepository.save(migrationOffer1, migrationOffer2, migrationOffer3);

        removedOfferRepository.save(List.of(
            new RemovedOffer(
                sourceSelfOffer1.getId(),
                Instant.now().minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).minusSeconds(10),
                false, null, null),
            new RemovedOffer(
                sourceSelfOffer2.getId(),
                Instant.now().minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).minusSeconds(10),
                false, null, null)
        ));

        removeOffersExecutor.doRealJob(null);

        final Offer resultTargetOffer1 = offerRepository.findOfferByBusinessSkuKey(targetOffer1.getBusinessSkuKey());
        final Offer resultTargetOffer2 = offerRepository.findOfferByBusinessSkuKey(targetOffer2.getBusinessSkuKey());
        final Offer anotherBusinessTargetOffer =
            offerRepository.findOfferByBusinessSkuKey(targetOffer3.getBusinessSkuKey());

        assertEquals(Offer.MappingConfidence.PARTNER_SELF, resultTargetOffer1.getApprovedSkuMappingConfidence());
        assertEquals(Offer.MappingConfidence.PARTNER_SELF, resultTargetOffer2.getApprovedSkuMappingConfidence());
        assertEquals(
            targetOffer3.getApprovedSkuMappingConfidence(),
            anotherBusinessTargetOffer.getApprovedSkuMappingConfidence()
        );
    }

    @Test
    public void shouldLookingForExistingTargetOffer() throws Exception {
        final Supplier targetSupplier = supplierRepository.insert(
            OfferTestUtils.simpleSupplier()
                .setId(43)
        );

        int notExistingTargetOfferId = 666;

        final Offer sourceSelfOffer1 = OfferTestUtils.simpleOffer(1)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);

        final Offer targetOffer1 = OfferTestUtils.simpleOffer(11)
            .setBusinessId(targetSupplier.getId())
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.MIGRATED);

        offerRepository.insertOffers(sourceSelfOffer1, targetOffer1);

        final MigrationStatus freshMigrationStatus = new MigrationStatus()
            .setId(1L)
            .setSourceBusinessId(sourceSelfOffer1.getBusinessId())
            .setTargetBusinessId(notExistingTargetOfferId)
            .setSupplierId(111)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.FINISHED);

        final MigrationStatus oldMigrationStatus = new MigrationStatus()
            .setId(2L)
            .setSourceBusinessId(sourceSelfOffer1.getBusinessId())
            .setTargetBusinessId(targetOffer1.getBusinessId())
            .setSupplierId(222)
            .setCreatedTs(Instant.now().minusSeconds(1))
            .setMigrationStatus(MigrationStatusType.FINISHED);

        migrationStatusRepository.save(freshMigrationStatus, oldMigrationStatus);

        final MigrationOffer freshMigrationOffer = new MigrationOffer()
            .setMigrationId(freshMigrationStatus.getId())
            .setShopSku(sourceSelfOffer1.getShopSku())
            .setState(MigrationOfferState.PROCESSED);

        final MigrationOffer oldMigrationOffer = new MigrationOffer()
            .setMigrationId(oldMigrationStatus.getId())
            .setShopSku(sourceSelfOffer1.getShopSku())
            .setState(MigrationOfferState.PROCESSED);

        migrationOfferRepository.save(freshMigrationOffer, oldMigrationOffer);

        removedOfferRepository.save(List.of(
            new RemovedOffer(
                sourceSelfOffer1.getId(),
                Instant.now().minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).minusSeconds(10),
                false, null, null)
        ));

        removeOffersExecutor.doRealJob(null);

        final Offer resultTargetOffer = offerRepository.findOfferByBusinessSkuKey(targetOffer1.getBusinessSkuKey());
        final Offer sourceOffer = offerRepository.findOfferByBusinessSkuKey(sourceSelfOffer1.getBusinessSkuKey());

        assertNull(sourceOffer);
        assertNotNull(resultTargetOffer);
        assertEquals(targetOffer1.getId(), resultTargetOffer.getId());
        assertEquals(Offer.MappingConfidence.PARTNER_SELF, resultTargetOffer.getApprovedSkuMappingConfidence());
    }

    @Test
    public void shouldNotTransferNotMigratedOfferRights() throws Exception {
        final Offer sourceSelfOffer1 = OfferTestUtils.simpleOffer(1)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("TestShopSku1")
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);

        offerRepository.insertOffers(sourceSelfOffer1);

        removedOfferRepository.save(List.of(
            new RemovedOffer(
                sourceSelfOffer1.getId(),
                Instant.now().minusSeconds(RemoveOffersExecutor.DELAY_TO_REMOVE).minusSeconds(10),
                false, null, null)
        ));

        removeOffersExecutor.doRealJob(null);

        assertTrue(offerRepository.findAll().isEmpty());
    }
}
