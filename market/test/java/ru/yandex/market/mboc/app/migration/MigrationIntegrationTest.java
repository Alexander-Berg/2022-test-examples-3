package ru.yandex.market.mboc.app.migration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.pipeline.datacamp.BaseDatacampPipelineTest;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationRemovedOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferMeta;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest;
import ru.yandex.market.mboc.common.services.migration.BusinessMigrationServiceImpl;
import ru.yandex.market.mboc.common.services.migration.BusinessMigrationServiceImplTest;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerToProcess;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

/**
 * @author shadoff
 * created on 3/1/21
 */
public class MigrationIntegrationTest extends BaseDatacampPipelineTest {
    private static final int DST_BIZ = 1;
    private static final int SRC_BIZ = 2;
    private static final int WHITE_SHOP_ID = 4;
    private static final int OTHER_WHITE_SHOP_ID = 5;
    private static final int ANOTHER_OTHER_WHITE_SHOP_ID = 6;
    private static final int BLUE_SHOP_ID = 7;
    private static final int OTHER_BLUE_SHOP_ID = 8;
    private static final String SSKU1 = "ssku1";
    private static final String SSKU2 = "ssku2";
    private static final String SSKU3 = "ssku3";
    private static final String SSKU4 = "ssku4";
    private static final String SSKU5 = "ssku5";
    private static final long MAPPING_MODEL_ID = 1234L;
    private static final long CATEGORY_ID = TEST_CATEGORY_INFO_ID;
    private static final long CATEGORY_ID_NO_KNOWLEDGE = TEST_CATEGORY_INFO_ID + 1;
    private static final String CATEGORY_NAME = "category";

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private MigrationOfferRepository migrationOfferRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;

    private BusinessMigrationServiceImpl businessMigrationService;

    @Before
    public void setUpMigration() {
        SupplierConverterServiceMock supplierConverterService = new SupplierConverterServiceMock();
        DataCampIdentifiersService dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID, SupplierConverterServiceMock.BERU_BUSINESS_ID,
            supplierConverterService);

        businessMigrationService = new BusinessMigrationServiceImpl(migrationService, migrationStatusRepository,
            transactionHelper, dataCampIdentifiersService, supplierService,
            Mockito.mock(ComplexMonitoring.class), migrationOfferRepository, offerRepository);

        prepareCategories();
        prepareSuppliers();
        disableInMigrationCache();
        enableNotAllowWrongTargetResolution();

        migrationService.invalidateAll();
    }

    @Test
    public void testWhiteMigration() {
        insertWhiteOffer(SRC_BIZ, SSKU1, List.of(WHITE_SHOP_ID, OTHER_WHITE_SHOP_ID));
        insertWhiteOffer(SRC_BIZ, SSKU2, List.of(OTHER_WHITE_SHOP_ID));
        insertWhiteOffer(SRC_BIZ, SSKU3, List.of(WHITE_SHOP_ID));
        insertWhiteOffer(SRC_BIZ, SSKU5, List.of(WHITE_SHOP_ID));

        insertWhiteOffer(DST_BIZ, SSKU4, List.of(ANOTHER_OTHER_WHITE_SHOP_ID));
        insertWhiteOffer(DST_BIZ, SSKU5, List.of(ANOTHER_OTHER_WHITE_SHOP_ID));

        long migrationId = lock(WHITE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId3 = merge(SSKU3, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId5 = merge(SSKU5, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        receiveOffer(migrationOfferId1, SSKU1, WHITE_SHOP_ID);

        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.ACTIVE);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVING);

        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVING);

        receiveOffer(migrationOfferId3, SSKU3, WHITE_SHOP_ID);
        receiveOffer(migrationOfferId5, SSKU5, WHITE_SHOP_ID);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);

        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);

        linkSupplierToBusiness(WHITE_SHOP_ID);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkWhiteOffer(DST_BIZ, SSKU1, WHITE_SHOP_ID);
        checkWhiteOffer(SRC_BIZ, SSKU1, OTHER_WHITE_SHOP_ID);

        checkWhiteOffer(SRC_BIZ, SSKU2, OTHER_WHITE_SHOP_ID);

        checkWhiteOffer(DST_BIZ, SSKU3, WHITE_SHOP_ID);
        checkWhiteOffer(SRC_BIZ, SSKU3, List.of());

        checkWhiteOffer(DST_BIZ, SSKU4, List.of(ANOTHER_OTHER_WHITE_SHOP_ID));

        checkWhiteOffer(SRC_BIZ, SSKU5, List.of());
        checkWhiteOffer(DST_BIZ, SSKU5, List.of(WHITE_SHOP_ID, ANOTHER_OTHER_WHITE_SHOP_ID));
    }

    @Test
    public void testMigrationWithFinish() {
        enableAllowUnlockBeforeOffersReceive();
        insertWhiteOffer(SRC_BIZ, SSKU1, List.of(WHITE_SHOP_ID, OTHER_WHITE_SHOP_ID));
        insertWhiteOffer(SRC_BIZ, SSKU3, List.of(WHITE_SHOP_ID));

        long migrationId = lock(WHITE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId3 = merge(SSKU3, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVING);

        linkSupplierToBusiness(WHITE_SHOP_ID);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.SUCCESS);

        asyncFinish(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVING);

        receiveOffer(migrationOfferId1, SSKU1, WHITE_SHOP_ID);
        receiveOffer(migrationOfferId3, SSKU3, WHITE_SHOP_ID);

        asyncFinish(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVING);

        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        asyncFinish(WHITE_SHOP_ID, BusinessMigration.Status.SUCCESS);

        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkWhiteOffer(DST_BIZ, SSKU1, WHITE_SHOP_ID);
        checkWhiteOffer(SRC_BIZ, SSKU1, OTHER_WHITE_SHOP_ID);

        checkWhiteOffer(DST_BIZ, SSKU3, WHITE_SHOP_ID);
        checkWhiteOffer(SRC_BIZ, SSKU3, List.of());
    }

    @Test
    public void testBlueMigrationCopyStatus() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN,
            offer -> offer
                .updateAcceptanceStatusForTests(BLUE_SHOP_ID, Offer.AcceptanceStatus.NEW)
                .updateAcceptanceStatusForTests(OTHER_BLUE_SHOP_ID, Offer.AcceptanceStatus.NEW)
        );
        insertBlueOffer(SRC_BIZ, SSKU2, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.IN_CLASSIFICATION,
            offer -> offer.setBindingKind(Offer.BindingKind.SUGGESTED));
        insertBlueOffer(SRC_BIZ, SSKU3, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.IN_MODERATION,
            offer -> offer
                .setSupplierSkuMapping(new Offer.Mapping(MARKET_SKU_ID_1, LocalDateTime.now(), Offer.SkuType.MARKET))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW));
        insertBlueOffer(SRC_BIZ, SSKU4, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.IN_PROCESS);

        // create tickets
        tmsCreateTrackerTickets().accept(null);
        var sourceOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(SRC_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));
        Assert.assertNull(sourceOffers.get(SSKU1).getTrackerTicket());
        Assert.assertNotNull(sourceOffers.get(SSKU2).getTrackerTicket());
        // Moderation has no tickets
        Assert.assertNull(sourceOffers.get(SSKU3).getProcessingTicketId());
        Assert.assertNotNull(sourceOffers.get(SSKU4).getTrackerTicket());

        long migrationId = lock(BLUE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId2 = merge(SSKU2, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId3 = merge(SSKU3, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId4 = merge(SSKU4, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        receiveOffer(migrationOfferId1, SSKU1, BLUE_SHOP_ID);
        receiveOffer(migrationOfferId2, SSKU2, BLUE_SHOP_ID);
        receiveOffer(migrationOfferId3, SSKU3, BLUE_SHOP_ID);
        receiveOffer(migrationOfferId4, SSKU4, BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(DST_BIZ, SSKU1, BLUE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU1, OTHER_BLUE_SHOP_ID);

        checkBlueOffer(DST_BIZ, SSKU2, BLUE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU2, OTHER_BLUE_SHOP_ID);

        checkBlueOffer(DST_BIZ, SSKU3, BLUE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU3, OTHER_BLUE_SHOP_ID);

        checkBlueOffer(DST_BIZ, SSKU4, BLUE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU4, OTHER_BLUE_SHOP_ID);

        tmsCreateTrackerTickets().accept(null);
        var dstOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(DST_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        Assert.assertNull(dstOffers.get(SSKU1).getTrackerTicket());
        Assert.assertEquals(Offer.ProcessingStatus.OPEN, dstOffers.get(SSKU1).getProcessingStatus());

        Assert.assertNotNull(dstOffers.get(SSKU2).getTrackerTicket());
        Assert.assertNotEquals(sourceOffers.get(SSKU2).getTrackerTicket(), dstOffers.get(SSKU2).getTrackerTicket());
        Assert.assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, dstOffers.get(SSKU2).getProcessingStatus());

        // Moderation has no tickets
        Assert.assertNull(dstOffers.get(SSKU3).getProcessingTicketId());
        Assert.assertEquals(Offer.ProcessingStatus.IN_MODERATION, dstOffers.get(SSKU3).getProcessingStatus());

        Assert.assertNotNull(dstOffers.get(SSKU4).getTrackerTicket());
        Assert.assertNotEquals(sourceOffers.get(SSKU4).getTrackerTicket(), dstOffers.get(SSKU4).getTrackerTicket());
        Assert.assertEquals(Offer.ProcessingStatus.IN_PROCESS, dstOffers.get(SSKU4).getProcessingStatus());
    }

    @Test
    public void testBlueMigrationTestResolutions() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.IN_PROCESS);
        insertBlueOffer(DST_BIZ, SSKU1, List.of(OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.IN_CLASSIFICATION,
            o -> o.setBindingKind(Offer.BindingKind.SUGGESTED));

        insertBlueOffer(SRC_BIZ, SSKU2, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.IN_PROCESS);

        insertBlueOffer(SRC_BIZ, SSKU3, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.NO_KNOWLEDGE, offer -> offer
            .setCategoryIdForTests(CATEGORY_ID_NO_KNOWLEDGE, Offer.BindingKind.APPROVED)
            .setClassifierCategoryId(CATEGORY_ID_NO_KNOWLEDGE, 0.99)
            .setAutomaticClassification(true));
        insertBlueOffer(DST_BIZ, SSKU3, List.of(OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.IN_PROCESS);

        insertBlueOffer(SRC_BIZ, SSKU4, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);

        // create tickets
        tmsCreateTrackerTickets().accept(null);

        var sourceOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(SRC_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        var oldDstOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(DST_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        long migrationId = lock(BLUE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET);
        long migrationOfferId2 = merge(SSKU2, BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET);
        long migrationOfferId3 = merge(SSKU3, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId4 = merge(SSKU4, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        receiveOffer(migrationOfferId1, SSKU1, BLUE_SHOP_ID);
        receiveOffer(migrationOfferId2, SSKU2, BLUE_SHOP_ID);
        receiveOffer(migrationOfferId3, SSKU3, BLUE_SHOP_ID);
        receiveOffer(migrationOfferId4, SSKU4, BLUE_SHOP_ID, false);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(SRC_BIZ, SSKU1, List.of());
        checkBlueOffer(DST_BIZ, SSKU1, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID));
        checkBlueOffer(DST_BIZ, SSKU2, BLUE_SHOP_ID);
        checkBlueOffer(DST_BIZ, SSKU3, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID));
        checkBlueOffer(DST_BIZ, SSKU4, BLUE_SHOP_ID);

        tmsCreateTrackerTickets().accept(null);

        var dstOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(DST_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        Assert.assertNotNull(dstOffers.get(SSKU1).getTrackerTicket());
        Assert.assertNotEquals(sourceOffers.get(SSKU1).getTrackerTicket(), dstOffers.get(SSKU1).getTrackerTicket());
        Assert.assertEquals(oldDstOffers.get(SSKU1).getTrackerTicket(), dstOffers.get(SSKU1).getTrackerTicket());
        Assert.assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, dstOffers.get(SSKU1).getProcessingStatus());

        // acceptance ok: OPEN->IN_MODERATION
        Assert.assertEquals(Offer.ProcessingStatus.IN_MODERATION, dstOffers.get(SSKU2).getProcessingStatus());

        Assert.assertEquals(Offer.ProcessingStatus.NO_KNOWLEDGE, dstOffers.get(SSKU3).getProcessingStatus());

        Assert.assertNotNull(dstOffers.get(SSKU4).getTrackerTicket());
        Assert.assertEquals(Offer.ProcessingStatus.IN_PROCESS, dstOffers.get(SSKU4).getProcessingStatus());

        sourceOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(SRC_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));
        offersProcessingStatusService.processOffers(sourceOffers.values());

        dstOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(DST_BIZ))
            .stream().collect(Collectors.toMap(Offer::getShopSku, Function.identity()));

        Assert.assertEquals(sourceOffers.get(SSKU4).getProcessingStatus(), dstOffers.get(SSKU4).getProcessingStatus());
        Assert.assertEquals(Offer.ProcessingStatus.IN_PROCESS, dstOffers.get(SSKU4).getProcessingStatus());
    }

    @Test
    public void testInitialBlueMigration() {
        int notMigratedBlueId = 123432;
        Supplier blueSupplier = new Supplier(notMigratedBlueId,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setFulfillment(true)
            .setDatacamp(false);
        supplierRepository.insert(blueSupplier);

        insertBlueOffer(notMigratedBlueId, SSKU1, List.of(notMigratedBlueId), Offer.ProcessingStatus.OPEN,
            o -> o.setDataCampOffer(false)
                .updateAcceptanceStatusForTests(notMigratedBlueId, Offer.AcceptanceStatus.NEW));
        insertBlueOffer(notMigratedBlueId, SSKU2, List.of(notMigratedBlueId), Offer.ProcessingStatus.IN_CLASSIFICATION,
            o -> o.setDataCampOffer(false)
                .setBindingKind(Offer.BindingKind.SUGGESTED));
        insertBlueOffer(notMigratedBlueId, SSKU3, List.of(notMigratedBlueId), Offer.ProcessingStatus.IN_MODERATION,
            o -> o.setDataCampOffer(false)
                .setSupplierSkuMapping(new Offer.Mapping(MARKET_SKU_ID_1, LocalDateTime.now(), Offer.SkuType.MARKET))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW));
        insertBlueOffer(notMigratedBlueId, SSKU4, List.of(notMigratedBlueId), Offer.ProcessingStatus.IN_PROCESS,
            o -> o.setDataCampOffer(false));

        tmsCreateTrackerTickets();
        Map<Long, Offer> sourceOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(notMigratedBlueId))
            .stream().collect(Collectors.toMap(Offer::getId, Function.identity()));

        long migrationId = lock(notMigratedBlueId, notMigratedBlueId, DST_BIZ);

        Assert.assertTrue(migrationService.isInMigrationCached(notMigratedBlueId));

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId2 = merge(SSKU2, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId3 = merge(SSKU3, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId4 = merge(SSKU4, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);
        receiveOffer(migrationOfferId2, SSKU2, notMigratedBlueId);
        receiveOffer(migrationOfferId3, SSKU3, notMigratedBlueId);
        receiveOffer(migrationOfferId4, SSKU4, notMigratedBlueId);

        unlock(notMigratedBlueId, notMigratedBlueId, DST_BIZ, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(notMigratedBlueId);

        unlock(notMigratedBlueId, notMigratedBlueId, DST_BIZ, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(DST_BIZ, SSKU1, notMigratedBlueId);
        checkBlueOffer(DST_BIZ, SSKU2, notMigratedBlueId);
        checkBlueOffer(DST_BIZ, SSKU3, notMigratedBlueId);
        checkBlueOffer(DST_BIZ, SSKU4, notMigratedBlueId);

        List<Offer> dstOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(DST_BIZ));
        Assertions.assertThat(dstOffers).extracting(Offer::getId).containsExactlyInAnyOrderElementsOf(
            sourceOffers.values().stream().map(Offer::getId).collect(Collectors.toSet()));

        dstOffers.forEach(offer -> {
            Offer source = sourceOffers.get(offer.getId());
            Assert.assertEquals(source.getProcessingStatus(), offer.getProcessingStatus());
            Assert.assertEquals(source.getTrackerTicket(), offer.getTrackerTicket());
            Assert.assertTrue(offer.isDataCampOffer());
        });

        migrationService.checkAndUpdateCache();
        dstOffers.stream().filter(o -> o.getProcessingStatus() == Offer.ProcessingStatus.IN_MODERATION)
            .peek(offer -> {
                operatorModeratesMapping(
                    SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.ACCEPTED,
                    MARKET_SKU_ID_1, null)
                    .accept(offer);
            })
            .map(o -> offerRepository.getOfferById(o.getId()))
            .forEach(o -> {
                Assert.assertEquals(Offer.ProcessingStatus.PROCESSED, o.getProcessingStatus());
                long mappingId = o.getApprovedSkuMapping().getMappingId();
                Assert.assertEquals(MARKET_SKU_ID_1, mappingId);
            });

        var newSourceOffers = offerRepository.findOffers(
            new OffersFilter().setBusinessIds(notMigratedBlueId));
        Assertions.assertThat(newSourceOffers).isEmpty();

        Assertions.assertThat(migrationRemovedOfferRepository.findAll()).isEmpty();
    }

    @Test
    public void testInitialBlueMigrationOnExistingOffer() {
        int notMigratedBlueId = 123432;
        Supplier blueSupplier = new Supplier(notMigratedBlueId,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(false);
        supplierRepository.insert(blueSupplier);

        insertBlueOffer(notMigratedBlueId, SSKU1, List.of(notMigratedBlueId), Offer.ProcessingStatus.OPEN,
            o -> o.setDataCampOffer(false));
        insertBlueOffer(DST_BIZ, SSKU1, List.of(ANOTHER_OTHER_WHITE_SHOP_ID), Offer.ProcessingStatus.OPEN);

        Set<Long> sourceIds = offerRepository.findOffers(new OffersFilter().setBusinessIds(notMigratedBlueId)).stream()
            .map(Offer::getId).collect(Collectors.toSet());

        long migrationId = lock(notMigratedBlueId, notMigratedBlueId, DST_BIZ);

        Assert.assertTrue(migrationService.isInMigrationCached(notMigratedBlueId));

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET);
        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);

        unlock(notMigratedBlueId, notMigratedBlueId, DST_BIZ, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(notMigratedBlueId);

        unlock(notMigratedBlueId, notMigratedBlueId, DST_BIZ, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(DST_BIZ, SSKU1, List.of(notMigratedBlueId, ANOTHER_OTHER_WHITE_SHOP_ID));

        Set<Long> dstIds = offerRepository.findOffers(new OffersFilter().setBusinessIds(DST_BIZ)).stream()
            .map(Offer::getId).collect(Collectors.toSet());
        Assertions.assertThat(dstIds).doesNotContainAnyElementsOf(sourceIds);

        var newSourceOffers = offerRepository.findOffers(
            new OffersFilter().setBusinessIds(notMigratedBlueId));
        Assertions.assertThat(newSourceOffers).isEmpty();

        var removedOffers = migrationRemovedOfferRepository.findAll().stream()
            .collect(Collectors.toMap(MigrationRemovedOffer::getMigrationOfferId, Function.identity()));
        migrationOfferRepository.findAll()
            .forEach(offer -> Assertions.assertThat(removedOffers.get(offer.getId())).isNotNull());
    }

    @Test
    public void testWhiteMigratesFromBlue() {
        Supplier whiteShop = supplierRepository.findById(WHITE_SHOP_ID);
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN, offer ->
            offer.addNewServiceOfferIfNotExistsForTests(whiteShop));
        insertBlueOffer(SRC_BIZ, SSKU2, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.PROCESSED, offer ->
            offer.addNewServiceOfferIfNotExistsForTests(whiteShop));

        long migrationId = lock(WHITE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        long migrationOfferId2 = merge(SSKU2, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        receiveOffer(migrationOfferId1, SSKU1, WHITE_SHOP_ID, false);
        receiveOffer(migrationOfferId2, SSKU2, WHITE_SHOP_ID, false);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(WHITE_SHOP_ID);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(SRC_BIZ, SSKU1, BLUE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU2, BLUE_SHOP_ID);

        checkWhiteOffer(DST_BIZ, SSKU1, WHITE_SHOP_ID);
        checkWhiteOffer(DST_BIZ, SSKU2, WHITE_SHOP_ID);
    }

    @Test
    public void testMigrateToTargetValidateError() {
        Supplier whiteShop = supplierRepository.findById(WHITE_SHOP_ID);
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN, offer ->
            offer.addNewServiceOfferIfNotExistsForTests(whiteShop));

        long migrationId = lock(WHITE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET);

        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(DST_BIZ, SSKU1)
            .get()
            .setContent(offerToProcess().getContentBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketSkuId(MARKET_SKU_ID_1)
                        .setMarketModelId(MODEL_PARENT_ID)
                        .setMarketCategoryId(Math.toIntExact(TEST_CATEGORY_INFO_ID))
                        .build())
                    .setUcMapping(OfferBuilder.categoryMapping(CATEGORY_ID, CATEGORY_NAME))
                    .build())
                // set empty title
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setActual(
                    offerToProcess().getContent().getPartner().getActual().toBuilder().clearTitle()))
            )
            .build();

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(toMessage(WHITE_SHOP_ID, dcOfferReceived)));

        var migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        assertEquals(MigrationOfferState.RECEIVED, migrationOffer.getState());

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(WHITE_SHOP_ID);

        unlock(WHITE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(SRC_BIZ, SSKU1, BLUE_SHOP_ID);

        var dstOffers = offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(DST_BIZ, SSKU1));
        Assertions.assertThat(dstOffers).hasSize(1);
        Assert.assertEquals(Offer.ProcessingStatus.INVALID, dstOffers.get(0).getProcessingStatus());
    }

    @Test
    public void testMigrateFromSourceWithNonCriticalValidationError() {
        Supplier supplier = supplierRepository.findById(BLUE_SHOP_ID);
        supplier.setFulfillment(true);
        supplierRepository.update(supplier);

        enableBarcodeValidation();
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);
        Offer offer = offerRepository.findOfferByBusinessSkuKey(SRC_BIZ, SSKU1);
        Assert.assertNull(offer.getBarCode());

        long migrationId = lock(BLUE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(DST_BIZ, SSKU1)
            .get()
            .setContent(offerToProcess().getContentBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketSkuId(MARKET_SKU_ID_1)
                        .setMarketModelId(MODEL_PARENT_ID)
                        .setMarketCategoryId(Math.toIntExact(TEST_CATEGORY_INFO_ID))
                        .build())
                    .setUcMapping(OfferBuilder.categoryMapping(CATEGORY_ID, CATEGORY_NAME))
                    .build())
                // set empty barcode from migrator
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setActual(
                    offerToProcess().getContent().getPartner().getActual().toBuilder().clearBarcode()))
            )
            .build();

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(toMessage(BLUE_SHOP_ID, dcOfferReceived)));

        var migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        assertEquals(MigrationOfferState.RECEIVED, migrationOffer.getState());

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(DST_BIZ, SSKU1, BLUE_SHOP_ID);
    }

    @Test
    public void testAllowCreateUpdate() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID),
            Offer.ProcessingStatus.AUTO_PROCESSED);
        Offer offer = offerRepository.findOfferByBusinessSkuKey(SRC_BIZ, SSKU1);
        offer.updateApprovedSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.PARTNER20),
            Offer.MappingConfidence.PARTNER_SELF)
            .setDataCampOffer(true);
        offerRepository.updateOffers(offer);

        long migrationId = lock(BLUE_SHOP_ID);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        receiveOffer(migrationOfferId1, SSKU1, BLUE_SHOP_ID);

        var migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        assertEquals(MigrationOfferState.RECEIVED, migrationOffer.getState());

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkBlueOffer(SRC_BIZ, SSKU1, OTHER_BLUE_SHOP_ID);
        checkBlueOffer(DST_BIZ, SSKU1, BLUE_SHOP_ID);

        migrationService.checkAndUpdateCache();

        tmsSendDataCampOfferStates().accept(null);
        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(1);

        List<DataCampOffer.Offer> dataCampOffers = events.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        Assertions.assertThat(dataCampOffers).hasSize(2);

        Map<Integer, DataCampOffer.Offer> offersByBusiness =
            dataCampOffers.stream().collect(Collectors.toMap(o -> o.getIdentifiers().getBusinessId(),
                Function.identity()));
        var sourceEvent = offersByBusiness.get(SRC_BIZ);
        Assert.assertTrue(sourceEvent.getContent().getStatus().getContentSystemStatus().getAllowModelCreateUpdate());

        var dstEvent = offersByBusiness.get(DST_BIZ);
        // Should not be in content processing export queue
        Assertions.assertThat(contentProcessingQueueRepository
            .findAllByBusinessSkuKeys(new BusinessSkuKey(DST_BIZ, SSKU1)))
            .isEmpty();
        Assert.assertTrue(dstEvent.getContent().getStatus().getContentSystemStatus().getAllowModelCreateUpdate());
    }

    @Test
    public void testAllowCreateUpdateInitial() {
        int notMigratedBlueId = 123432;
        Supplier blueSupplier = new Supplier(notMigratedBlueId,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(false);
        supplierRepository.insert(blueSupplier);

        insertBlueOffer(notMigratedBlueId, SSKU1, List.of(notMigratedBlueId), Offer.ProcessingStatus.OPEN,
            o -> o.setDataCampOffer(false)
                .updateApprovedSkuMapping(
                    new Offer.Mapping(123, LocalDateTime.now(), Offer.SkuType.PARTNER20),
                    Offer.MappingConfidence.PARTNER_SELF));

        long migrationId = lock(notMigratedBlueId, notMigratedBlueId, DST_BIZ);
        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);
        unlock(notMigratedBlueId, notMigratedBlueId, DST_BIZ, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);
        linkSupplierToBusiness(notMigratedBlueId);
        unlock(notMigratedBlueId, notMigratedBlueId, DST_BIZ, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);
        checkBlueOffer(DST_BIZ, SSKU1, List.of(notMigratedBlueId));

        migrationService.checkAndUpdateCache();

        tmsSendDataCampOfferStates().accept(null);
        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(1);

        List<DataCampOffer.Offer> dataCampOffers = events.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        Assertions.assertThat(dataCampOffers).hasSize(1);

        Map<Integer, DataCampOffer.Offer> offersByBusiness =
            dataCampOffers.stream().collect(Collectors.toMap(o -> o.getIdentifiers().getBusinessId(),
                Function.identity()));
        var dstEvent = offersByBusiness.get(DST_BIZ);
        Assert.assertTrue(dstEvent.getContent().getStatus().getContentSystemStatus().getAllowModelCreateUpdate());
    }

    @Test
    public void testDuplicateEventsFromDcInInitialMigration() {
        int notMigratedBlueId = 123432;
        Supplier blueSupplier = new Supplier(notMigratedBlueId,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(false);
        supplierRepository.insert(blueSupplier);

        insertBlueOffer(notMigratedBlueId, SSKU1, List.of(notMigratedBlueId), Offer.ProcessingStatus.OPEN);
        lock(notMigratedBlueId, notMigratedBlueId, DST_BIZ);

        Assert.assertTrue(migrationService.isInMigrationCached(notMigratedBlueId));

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);
        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);
        MigrationOffer migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        Assertions.assertThat(migrationOffer.getErrorText()).isNull();
    }

    @Test
    public void testWrongSkuInitialMigration() {
        int notMigratedBlueId = 123432;
        Supplier blueSupplier = new Supplier(notMigratedBlueId,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(false);
        supplierRepository.insert(blueSupplier);

        modelStorageCachingService.clear();

        insertBlueOffer(notMigratedBlueId, SSKU1, List.of(notMigratedBlueId), Offer.ProcessingStatus.OPEN, offer ->
            offer.setSupplierSkuMapping(new Offer.Mapping(MARKET_SKU_ID_1, LocalDateTime.now()))
                .updateApprovedSkuMapping(null));
        lock(notMigratedBlueId, notMigratedBlueId, DST_BIZ);

        Assert.assertTrue(migrationService.isInMigrationCached(notMigratedBlueId));

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);
    }

    @Test
    public void finishMigrationMarksEvents() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);
        insertBlueOffer(SRC_BIZ, SSKU2, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);
        insertBlueOffer(SRC_BIZ, SSKU3, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);
        insertBlueOffer(DST_BIZ, SSKU4, List.of(OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);

        long migrationId = lock(BLUE_SHOP_ID);

        // updating offers
        List<Offer> offers = offerRepository.findAll();
        offers.forEach(o -> o.setTitle(o.getTitle() + "_new"));
        offerRepository.updateOffers(offers);

        tmsSendDataCampOfferStates().accept(null);
        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(0); // nothing sent yet

        List<OfferMeta> offerMetas = offerMetaRepository.findAll();
        Assertions.assertThat(offerMetas).extracting(OfferMeta::getOfferId)
            .containsExactlyInAnyOrderElementsOf(offers.stream().map(Offer::getId).collect(Collectors.toList()));
        Assertions.assertThat(offerMetas).extracting(OfferMeta::getChangedDuringMigrationBusinessId)
            .containsOnly(DST_BIZ, SRC_BIZ);

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        receiveOffer(migrationOfferId1, SSKU1, BLUE_SHOP_ID);
        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);
        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);
        checkBlueOffer(DST_BIZ, SSKU1, BLUE_SHOP_ID);

        tmsSendDataCampOfferStates().accept(null);
        events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(0); // nothing sent yet

        // cache updated
        migrationService.checkAndUpdateCache();
        // job marked
        finishMigrations();

        tmsSendDataCampOfferStates().accept(null);
        events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(1);

        List<DataCampOffer.Offer> dataCampOffers = events.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        Assertions.assertThat(dataCampOffers).hasSize(5);

        var byBizId = dataCampOffers.stream().collect(Collectors.groupingBy(o -> o.getIdentifiers().getBusinessId()));
        Assertions.assertThat(byBizId.get(SRC_BIZ)).extracting(o -> o.getIdentifiers().getOfferId())
            .containsExactlyInAnyOrder(SSKU1, SSKU2, SSKU3);

        Assertions.assertThat(byBizId.get(DST_BIZ)).extracting(o -> o.getIdentifiers().getOfferId())
            .containsExactlyInAnyOrder(SSKU1, SSKU4);
    }

    @Test
    public void testInitialMigrationWithAutoProcessed() {
        int notMigratedBlueId = 123432;
        Supplier blueSupplier = new Supplier(notMigratedBlueId,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(false);
        supplierRepository.insert(blueSupplier);

        modelStorageCachingService.clear();

        insertBlueOffer(notMigratedBlueId, SSKU1, List.of(notMigratedBlueId), Offer.ProcessingStatus.AUTO_PROCESSED,
            offer -> offer.updateApprovedSkuMapping(new Offer.Mapping(123L, LocalDateTime.now()))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF));
        lock(notMigratedBlueId, notMigratedBlueId, DST_BIZ);

        Assert.assertTrue(migrationService.isInMigrationCached(notMigratedBlueId));

        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        receiveOffer(migrationOfferId1, SSKU1, notMigratedBlueId);

        Offer offer = offerRepository.findOfferByBusinessSkuKey(DST_BIZ, SSKU1);
        Assert.assertNotNull(offer.getMarketSpecificContentHash());
        Assert.assertEquals(offer.getMarketSpecificContentHash(), offer.getMarketSpecificContentHashSent());
    }

    @Test
    public void testWrongServiceOffersDuringMigration() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID), Offer.ProcessingStatus.IN_PROCESS);
        lock(BLUE_SHOP_ID);

        String someSsku = "QWER";
        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(BIZ_ID_SUPPLIER, someSsku)
            .get()
            .setContent(offerToProcess().getContentBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketSkuId(MARKET_SKU_ID_1)
                        .setMarketModelId(MODEL_PARENT_ID)
                        .setMarketCategoryId(Math.toIntExact(TEST_CATEGORY_INFO_ID))
                        .build())
                    .setUcMapping(OfferBuilder.categoryMapping(CATEGORY_ID, CATEGORY_NAME))
                    .build()))
            .build();

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(toMessage(BLUE_SHOP_ID, dcOfferReceived)));

        Offer offer = offerRepository.findOfferByBusinessSkuKey(BIZ_ID_SUPPLIER, someSsku);
        Assert.assertNull(offer);
    }

    @Test
    public void testMigrationServiceRemoved() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);

        long migrationId = lock(BLUE_SHOP_ID);
        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);

        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(DST_BIZ, SSKU1)
            .get();

        var service = OfferBuilder.create(initialOffer())
            .get()
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build())
                .build())
            .build();

        var removeMessage = LogbrokerDatacampOfferMessageHandlerDbTest.offer(
            dcOfferReceived,
            Function.identity(),
            basicOffer -> Map.of(BLUE_SHOP_ID, service, ANOTHER_OTHER_WHITE_SHOP_ID, DataCampOffer.Offer.newBuilder().build())
        );

        logbrokerDatacampOfferMessageHandler.process(List.of(removeMessage));

        var migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        assertEquals(MigrationOfferState.RECEIVED, migrationOffer.getState());

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkWhiteOffer(DST_BIZ, SSKU1, ANOTHER_OTHER_WHITE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU1, OTHER_BLUE_SHOP_ID);
    }

    @Test
    public void testMigrationServiceLost() {
        insertBlueOffer(SRC_BIZ, SSKU1, List.of(BLUE_SHOP_ID, OTHER_BLUE_SHOP_ID), Offer.ProcessingStatus.OPEN);

        long migrationId = lock(BLUE_SHOP_ID);
        long migrationOfferId1 = merge(SSKU1, BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET);

        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(DST_BIZ, SSKU1)
            .get();

        var removeMessage = LogbrokerDatacampOfferMessageHandlerDbTest.offer(
            dcOfferReceived,
            Function.identity(),
            basicOffer -> Map.of(ANOTHER_OTHER_WHITE_SHOP_ID, DataCampOffer.Offer.newBuilder().build())
        );

        logbrokerDatacampOfferMessageHandler.process(List.of(removeMessage));

        var migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        assertEquals(MigrationOfferState.NEW, migrationOffer.getState());

        var migrationStatus = migrationStatusRepository.getById(migrationId);
        migrationStatus.setCreatedTs(Instant.now()
            .minusSeconds(MigrationService.MIGRATION_IS_OLD_SECONDS).minusSeconds(1));
        migrationStatusRepository.save(migrationStatus);

        logbrokerDatacampOfferMessageHandler.process(List.of(removeMessage));

        migrationOffer = migrationOfferRepository.getById(migrationOfferId1);
        assertEquals(MigrationOfferState.RECEIVED, migrationOffer.getState());

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.IN_PROGRESS);
        finishMigrations();
        checkMigrationStatus(migrationId, MigrationStatusType.RECEIVED);

        linkSupplierToBusiness(BLUE_SHOP_ID);

        unlock(BLUE_SHOP_ID, BusinessMigration.Status.SUCCESS);
        checkMigrationStatus(migrationId, MigrationStatusType.FINISHED);

        checkWhiteOffer(DST_BIZ, SSKU1, ANOTHER_OTHER_WHITE_SHOP_ID);
        checkBlueOffer(SRC_BIZ, SSKU1, OTHER_BLUE_SHOP_ID);
    }

    private void linkSupplierToBusiness(int supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId);
        supplierRepository.update(supplier.setBusinessId(DST_BIZ));
    }

    private void checkMigrationStatus(long migrationId, MigrationStatusType migrationStatus) {
        var migration = migrationStatusRepository.getById(migrationId);
        assertEquals(migrationStatus, migration.getMigrationStatus());
    }

    private void checkWhiteOffer(int businessId, String shopSku, int serviceSupplierId) {
        checkReceivedOffer(businessId, shopSku, List.of(serviceSupplierId), MbocSupplierType.MARKET_SHOP);
    }

    private void checkWhiteOffer(int businessId, String shopSku, List<Integer> serviceSupplierIds) {
        checkReceivedOffer(businessId, shopSku, serviceSupplierIds, MbocSupplierType.MARKET_SHOP);
    }

    private void checkBlueOffer(int businessId, String shopSku, int serviceSupplierId) {
        checkReceivedOffer(businessId, shopSku, List.of(serviceSupplierId), MbocSupplierType.THIRD_PARTY);
    }

    private void checkBlueOffer(int businessId, String shopSku, List<Integer> serviceSupplierIds) {
        checkReceivedOffer(businessId, shopSku, serviceSupplierIds, MbocSupplierType.THIRD_PARTY);
    }

    private void checkReceivedOffer(int businessId, String shopSku, List<Integer> serviceSupplierIds,
                                    MbocSupplierType supplierType) {
        var dstOffers = offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(businessId, shopSku));
        assertThat(dstOffers).hasSize(1);
        var dstOffer = dstOffers.get(0);
        assertThat(dstOffer.getServiceOffers()).hasSize(serviceSupplierIds.size());
        serviceSupplierIds.forEach(supplierId -> {
            assertTrue(dstOffer.getServiceOffer(supplierId).isPresent());
            var serviceOffer = dstOffer.getServiceOffer(supplierId).get();
            assertEquals(supplierType, serviceOffer.getSupplierType());
        });
    }

    private void receiveOffer(long migrationOfferId, String shopSku, int supplierId) {
        receiveOffer(migrationOfferId, shopSku, supplierId, true);
    }

    private void receiveOffer(long migrationOfferId, String shopSku, int supplierId,
                              boolean addPartner) {
        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(DST_BIZ, shopSku)
            .get()
            .setContent(offerToProcess().getContentBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(addPartner ? DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketSkuId(MARKET_SKU_ID_1)
                        .setMarketModelId(MODEL_PARENT_ID)
                        .setMarketCategoryId(Math.toIntExact(TEST_CATEGORY_INFO_ID))
                        .build() : DataCampOfferMapping.Mapping.newBuilder().build())
                    .setUcMapping(OfferBuilder.categoryMapping(CATEGORY_ID, CATEGORY_NAME))
                    .build()))
            .build();

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(toMessage(supplierId, dcOfferReceived)));

        var migrationOffer = migrationOfferRepository.getById(migrationOfferId);
        assertEquals(MigrationOfferState.RECEIVED, migrationOffer.getState());
    }

    private void unlock(int supplierId, int source, int target, BusinessMigration.Status status) {
        var unlock = sendUnlock(supplierId, source, target);
        assertEquals(status, unlock.getStatus());
    }

    private void unlock(int supplierId, BusinessMigration.Status status) {
        var unlock = sendUnlock(supplierId, SRC_BIZ, DST_BIZ);
        assertEquals(status, unlock.getStatus());
    }

    private void asyncFinish(int supplierId, BusinessMigration.Status status) {
        var unlock = sendAsyncFinish(supplierId);
        assertEquals(status, unlock.getStatus());
    }


    private long merge(String shopSku,
                       BusinessMigration.ConflictResolutionStrategy strategy) {
        var merge = sendMerge(shopSku, strategy);
        assertTrue(merge.getSuccess());

        var migrationOffers = migrationOfferRepository.find(
            MigrationOfferRepository.Filter.builder()
                .shopSkus(List.of(shopSku))
                .build());
        assertThat(migrationOffers).hasSize(1);
        var migrationOffer = migrationOffers.get(0);
        assertEquals(MigrationOfferState.NEW, migrationOffer.getState());

        return migrationOffer.getId();
    }

    private long lock(int supplierId, Integer source, Integer target) {
        var lock = sendLock(supplierId, source, target);
        assertEquals(BusinessMigration.Status.IN_PROGRESS, lock.getStatus());

        var migrations = migrationStatusRepository.findAll();
        assertThat(migrations).hasSize(1);
        var migration = migrations.get(0);
        long migrationId = migration.getId();
        assertEquals(MigrationStatusType.NEW, migration.getMigrationStatus());

        migrationService.checkAndUpdateCache();

        hackToTheFuture();
        lock = sendLock(supplierId, source, target);
        assertEquals(BusinessMigration.Status.SUCCESS, lock.getStatus());

        migration = migrationStatusRepository.getById(migrationId);
        assertEquals(MigrationStatusType.ACTIVE, migration.getMigrationStatus());
        assertEquals(target, migration.getTargetBusinessId());
        assertEquals(source, migration.getSourceBusinessId());
        return migrationId;
    }

    private long lock(int supplierId) {
        return lock(supplierId, SRC_BIZ, DST_BIZ);
    }

    /**
     * like MigrationFinishExecutor does
     */
    public void finishMigrations() {
        var statusFilter = MigrationStatusRepository.Filter.builder()
            .migrationStatuses(EnumSet.of(MigrationStatusType.RECEIVING)).build();

        List<MigrationStatus> migrationStatuses = migrationStatusRepository.find(statusFilter);
        boolean ok = migrationStatuses.stream()
            .allMatch(migrationService::finishMigration);

        assertTrue("all migrations finished successfully", ok);

        migrationService.markEventsForFinished();
    }

    public BusinessMigration.LockBusinessResponse sendLock(int supplierId, int source, int target) {
        List<BusinessMigration.LockBusinessResponse> responses = new ArrayList<>();
        var observer = BusinessMigrationServiceImplTest.lockObserver(responses);

        var request = BusinessMigration.LockBusinessRequest.newBuilder()
            .setDstBusinessId(target)
            .setSrcBusinessId(source)
            .setShopId(supplierId)
            .build();

        businessMigrationService.lock(request, observer);

        assertThat(responses).hasSize(1);
        return responses.get(0);
    }

    public BusinessMigration.UnlockBusinessResponse sendUnlock(int supplierId, int source, int target) {
        List<BusinessMigration.UnlockBusinessResponse> responses = new ArrayList<>();
        var observer = BusinessMigrationServiceImplTest.unlockObserver(responses);

        var request = BusinessMigration.UnlockBusinessRequest.newBuilder()
            .setDstBusinessId(target)
            .setSrcBusinessId(source)
            .setShopId(supplierId)
            .build();

        businessMigrationService.unlock(request, observer);

        assertThat(responses).hasSize(1);
        return responses.get(0);
    }

    public BusinessMigration.AsyncFinishBusinessResponse sendAsyncFinish(int supplierId) {
        List<BusinessMigration.AsyncFinishBusinessResponse> responses = new ArrayList<>();
        var observer = BusinessMigrationServiceImplTest.asyncFinishObserver(responses);

        var request = BusinessMigration.AsyncFinishBusinessRequest.newBuilder()
            .setDstBusinessId(DST_BIZ)
            .setSrcBusinessId(SRC_BIZ)
            .setShopId(supplierId)
            .build();

        businessMigrationService.asyncFinish(request, observer);

        assertThat(responses).hasSize(1);
        return responses.get(0);
    }

    public BusinessMigration.MergeOffersResponse sendMerge(String shopSku,
                                                           BusinessMigration.ConflictResolutionStrategy strategy) {
        List<BusinessMigration.MergeOffersResponse> responses = new ArrayList<>();
        var observer = BusinessMigrationServiceImplTest.mergeObserver(responses);

        var unitedOffer = unitedOffer(DST_BIZ, shopSku);

        var request = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setResult(unitedOffer)
                .setConflictResolutionStrategy(strategy)
                .build())
            .setDryRun(false)
            .build();
        businessMigrationService.merge(request, observer);

        assertThat(responses).hasSize(1);
        return responses.get(0);
    }

    private void prepareCategories() {
        Category categoryNoKnowledge = new Category().setCategoryId(CATEGORY_ID_NO_KNOWLEDGE)
            .setAcceptGoodContent(true)
            .setAcceptContentFromWhiteShops(true)
            .setHasKnowledge(false)
            .setAcceptPartnerSkus(true);
        categoryCachingService.addCategory(categoryNoKnowledge);
        updateCategoryKnowledgeInRepo(CATEGORY_ID_NO_KNOWLEDGE, false);

        var info = OfferTestUtils.categoryInfoWithManualAcceptance()
            .setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setDsbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.insertOrUpdate(info);
    }

    public void prepareSuppliers() {
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
        Supplier whiteSupplier = new Supplier(WHITE_SHOP_ID,
            "WHITE_SHOP_ID",
            "sup.biz",
            "biz org",
            MbocSupplierType.MARKET_SHOP)
            .setDatacamp(true)
            .setBusinessId(SRC_BIZ);
        Supplier otherWhiteSupplier = new Supplier(OTHER_WHITE_SHOP_ID,
            "OTHER_WHITE_SHOP_ID",
            "sup.biz",
            "biz org",
            MbocSupplierType.MARKET_SHOP)
            .setDatacamp(true)
            .setBusinessId(SRC_BIZ);
        Supplier anotherOtherWhiteSupplier = new Supplier(ANOTHER_OTHER_WHITE_SHOP_ID,
            "ANOTHER_OTHER_WHITE_SHOP_ID",
            "sup.biz",
            "biz org",
            MbocSupplierType.MARKET_SHOP)
            .setDatacamp(true)
            .setBusinessId(DST_BIZ);

        Supplier blueSupplier = new Supplier(BLUE_SHOP_ID,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(true)
            .setFulfillment(true)
            .setBusinessId(SRC_BIZ);
        Supplier otherBlueSupplier = new Supplier(OTHER_BLUE_SHOP_ID,
            "blue shop",
            "sup.biz",
            "biz org",
            MbocSupplierType.THIRD_PARTY)
            .setDatacamp(true)
            .setFulfillment(true)
            .setBusinessId(SRC_BIZ);
        supplierRepository.insertBatch(targetBusiness, sourceBusiness, whiteSupplier, otherWhiteSupplier,
            anotherOtherWhiteSupplier, blueSupplier, otherBlueSupplier);
    }

    public void insertWhiteOffer(int businessId, String shopSku, List<Integer> serviceSupplierIds) {
        var serviceOffers = serviceSupplierIds.stream()
            .map(id -> new Offer.ServiceOffer(id)
                .setSupplierType(MbocSupplierType.MARKET_SHOP))
            .collect(Collectors.toList());

        var offer = OfferTestUtils.nextOffer()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setShopCategoryName(CATEGORY_NAME)
            .setApprovedSkuMappingInternal(new Offer.Mapping(MAPPING_MODEL_ID, LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setServiceOffers(serviceOffers);
        offerRepository.insertOffers(offer);
    }

    public void insertBlueOffer(int businessId,
                                String shopSku,
                                List<Integer> serviceSupplierIds,
                                Offer.ProcessingStatus processingStatus) {
        insertBlueOffer(businessId, shopSku, serviceSupplierIds, processingStatus, UnaryOperator.identity());
    }

    public void insertBlueOffer(int businessId,
                                String shopSku,
                                List<Integer> serviceSupplierIds,
                                Offer.ProcessingStatus processingStatus,
                                UnaryOperator<Offer> modifyOffer) {
        var serviceOffers = serviceSupplierIds.stream()
            .map(id -> new Offer.ServiceOffer(id)
                .setSupplierType(MbocSupplierType.THIRD_PARTY)
                .setServiceAcceptance(Offer.AcceptanceStatus.OK))
            .collect(Collectors.toList());

        var offer = OfferTestUtils.nextOffer()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setShopCategoryName(CATEGORY_NAME)
            .updateProcessingStatusIfValid(processingStatus)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setServiceOffers(serviceOffers)
            .setDataCampOffer(true);
        offerRepository.insertOffers(modifyOffer.apply(offer));
    }

    private void disableInMigrationCache() {
        disableCacheIntervalMigrationService();
    }



    @SneakyThrows
    private void disableCacheIntervalMigrationService() {
        Field field = migrationService.getClass().getDeclaredField("CACHE_UPDATE_CHECK_INTERVAL_SECONDS");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(migrationService, 0L);
    }

    @SneakyThrows
    private void enableAllowUnlockBeforeOffersReceive() {
        Field field = businessMigrationService.getClass().getDeclaredField("allowUnlockBeforeOffersReceived");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(businessMigrationService, true);
    }

    @SneakyThrows
    private void enableBarcodeValidation() {
        Field field = addProductInfoHelperService.getClass().getDeclaredField("enableBarCodeValidation");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(addProductInfoHelperService, true);
    }

    private static DataCampUnitedOffer.UnitedOffer.Builder unitedOffer(int businessId, String shopSku) {
        var basicOffer = offer(businessId, shopSku);
        return DataCampUnitedOffer.UnitedOffer.newBuilder().setBasic(basicOffer);
    }

    private static DataCampOffer.Offer.Builder offer(int businessId, String shopSku) {
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId(shopSku)
                .setBusinessId(businessId))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(1)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                            .setTimestamp(DataCampOfferUtil.toTimestamp(DateTimeUtils.instantNow()))
                            .build())
                        .setMarketSkuId(MAPPING_MODEL_ID)))
            );
    }

    @SneakyThrows
    private void enableNotAllowWrongTargetResolution() {
        Field field = businessMigrationService.getClass().getDeclaredField("notAllowWrongTargetResolution");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(businessMigrationService, true);
    }

    @SneakyThrows
    private void hackToTheFuture() {
        Field field = businessMigrationService.getClass().getDeclaredField("CACHE_WARM_SECONDS_TO_WAIT");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(businessMigrationService, -1L);
    }
}
