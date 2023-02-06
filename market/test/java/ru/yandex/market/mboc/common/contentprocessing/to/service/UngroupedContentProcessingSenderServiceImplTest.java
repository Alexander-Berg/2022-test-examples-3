package ru.yandex.market.mboc.common.contentprocessing.to.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.ir.http.OfferContentProcessing.OfferContentProcessingRequest;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelChangeSource;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationModel;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.AG_BATCH_LIMIT_KEY;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.UngroupedContentProcessingSenderServiceImpl.BATCH_SIZE_KEY;
import static ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService.CONFIG_ENABLE_MODEL_ALLOW_PARTNER_CONTENT_KEY;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.AUTO_PROCESSED;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.CONTENT_PROCESSING;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.INVALID;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_CLASSIFICATION;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.NEED_CONTENT;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.PROCESSED;

public class UngroupedContentProcessingSenderServiceImplTest
    extends AbstractContentProcessingSenderServiceTest {
    @Before
    @Override
    public void setUp() {
        super.setUp();

        service = new AdaptedForTestingUngroupedContentProcessingSenderServiceImpl(
            queue,
            agService,
            transactionHelper,
            offerRepository,
            mskuRepository,
            categoryService,
            new SupplierService(supplierRepository),
            offersProcessingStatusService,
            needContentStatusService,
            dataCampConverterService,
            dataCampService,
            datacampImportService,
            keyValueService,
            contentProcessingLog,
            new ContextedOfferDestinationCalculator(categoryInfoCache, keyValueService),
            hashCalculator,
            migrationModelRepository
        );
    }

    @Test
    public void processedOnlyUngroupedOffers() {
        storageKeyValueService.putValue(BATCH_SIZE_KEY, 1);

        var ungrouped1 = offer(businessSupplier, "ungrouped1", NEED_CONTENT, null, 1L);
        var ungrouped2 = offer(businessSupplier, "ungrouped2", NEED_CONTENT, null, 1L);
        var grouped = offer(businessSupplier, "grouped", NEED_CONTENT, 11, 1L);
        offerRepository.insertOffers(ungrouped1, ungrouped2, grouped);

        configureDatacampResponse(Map.of(
            ungrouped1, 1L,
            ungrouped2, 1L,
            grouped, 1L
        ));
        configureAgResponses(0);

        makeReadyInQueue(grouped);
        assertThat(queue.findGroupsReady()).hasSize(1);
        queue.readAllWithoutGroupId(b -> assertThat(b).hasSize(2), 100, 100);

        service.sendFromQueue();

        assertThat(queue.findGroupsReady()).hasSize(1);
        queue.readAllWithoutGroupId(b -> {
            throw new AssertionError();
        }, 100, 100);

        verify(agServiceImpl, times(2)).startContentProcessing(any(), any());
    }

    @Test
    @Override
    public void testErrorWrongProcessingStatus() {
        var offer = offer(businessSupplier, "offer1", Offer.ProcessingStatus.IN_RE_SORT, null, 1L)
            .setMarketSpecificContentHashSent(1L);
        super.testErrorWrongProcessingStatus(offer, Config.builder().expectedQueueSize(0).build());
    }

    @Test
    @Override
    public void testBarcodeError() {
        var existingOffer1 = offer(businessSupplier, "existingOffer1", NEED_CONTENT, null, 1L)
            .setCategoryIdForTests(allowedCategoryWithBarcodes.getCategoryId(), Offer.BindingKind.APPROVED)
            .setBarCode(null)
            .addNewServiceOfferIfNotExistsForTests(fulfillmentSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        var existingOffer2 = offer(businessSupplier, "existingOffer2", NEED_CONTENT, null, 1L)
            .setCategoryIdForTests(allowedCategoryWithBarcodes.getCategoryId(), Offer.BindingKind.APPROVED)
            .setBarCode(null)
            .addNewServiceOfferIfNotExistsForTests(businessSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        testBarcodeError(existingOffer1, existingOffer2, Config.builder().expectedQueueSize(0).build());
    }

    @Test
    @Override
    public void testFirstPicError() {
        var offerWithError = offer(businessSupplier, "offerWithError", NEED_CONTENT, null, 1L);
        var offerGood = offer(businessSupplier, "offerGood", NEED_CONTENT, null, 1L);
        testFirstPicError(offerWithError, offerGood);
    }

    @Test
    @Override
    public void testNoPicturesNoError() {
        var offer = offer(businessSupplier, "offer", NEED_CONTENT, null, 1L);
        testNoPicturesNoError(offer);
    }

    @Test
    @Override
    public void testDequeueIfNotAllPicturesDownloaded() {
        var offer = offer(businessSupplier, "offer", NEED_CONTENT, null, 1L);
        testDequeueIfNotAllPicturesDownloaded(offer);
    }

    @Test
    @Override
    public void testDequeueWhenNotAllPicturesDownloadedEmptyActual() {
        var offer = offer(businessSupplier, "offer", NEED_CONTENT, null, 1L);
        testDequeueWhenNotAllPicturesDownloadedEmptyActual(offer);
    }

    @Test
    public void testOfferGroupChangedInDatacamp() {
        var offer1 = offer(businessSupplier, "offer1", CONTENT_PROCESSING, null, 1L);
        var offer2 = offer(businessSupplier, "offer2", NEED_CONTENT, null, 1L);
        offerRepository.insertOffers(offer1, offer2);
        configureDatacampResponse(Map.of(offer1, 1L, offer2, 1L));
        configureDatacampResponse(offer2.copy().setGroupId(22), 1L, __ -> {
        }, false);
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(0);
        queue.readAllWithoutGroupId(b -> assertThat(b).hasSize(2), 100, 100);
        assertThat(queue.findAll()).hasSize(2);


        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        makeReadyInQueue(offer2);
        assertThat(queue.findGroupsReady()).hasSize(1);
        queue.readAllWithoutGroupId(b -> assertThat(b).hasSize(0), 100, 100);
        assertThat(queue.findAll()).hasSize(1);
        verify(agServiceImpl, times(1)).startContentProcessing(any(), any());
        assertThat(result.getOffersFailed()).containsOnlyKeys(offer2.getBusinessSkuKey());
        assertThat(result.getOffersDequeued()).containsExactly(offer1.getBusinessSkuKey());
        assertThat(result.getOffersSent()).containsExactly(offer1.getBusinessSkuKey());
        assertThatLogWasWritten(1);
    }

    @Test
    @Override
    public void testDontSendAndIgnoreWhenHasContentMapping() {
        var offerBad = offer(businessSupplier, "offerBad", Offer.ProcessingStatus.PROCESSED, null, 1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT);
        var offerGood = offer(businessSupplier, "offerGood", CONTENT_PROCESSING, null, 1L);
        testDontSendAndIgnoreWhenHasContentMapping(offerBad, offerGood);
    }

    @Test
    @Override
    public void testSendAnyCsku() {
        var offerGood = offer(cskuSupplier, "offerGood", PROCESSED, null, 1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(2L, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.CONTENT);

        testSendAnyCsku(offerGood);
    }


    @Test
    @Override
    public void testSendCorrectAllowCreateUpdateForCskuWithManualInsertToQueue() {
        var offer = offer(cskuSupplier, "offerGood", PROCESSED, null, 1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(2L, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.CONTENT);

        testSendCorrectAllowCreateUpdateForCskuWithManualInsertToQueue(offer, true);
    }

    @Test
    @Override
    public void testSendMskuMarkedAllowPartnerContent() {
        storageKeyValueService.putValue(CONFIG_ENABLE_MODEL_ALLOW_PARTNER_CONTENT_KEY, true);
        storageKeyValueService.invalidateCache();

        var msku = createMsku(2L)
            .setAllowPartnerContent(true);
        mskuRepository.save(msku);

        var offerGood = offer(businessSupplier, "offerGood", PROCESSED, null, 1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(msku.getMarketSkuId(), Offer.SkuType.MARKET),
                Offer.MappingConfidence.CONTENT
            );

        testSendMskuMarkedAllowPartnerContent(offerGood);

        storageKeyValueService.putValue(CONFIG_ENABLE_MODEL_ALLOW_PARTNER_CONTENT_KEY, false);
        storageKeyValueService.invalidateCache();
    }

    @Test
    @Override
    public void testChooseValidationCheckWithHigherLevel() {
        // OK, will be sent
        var offerGood = offer(businessSupplier, "offerGood", CONTENT_PROCESSING, null, 1L);
        // has two problems: different hash + approved content mapping
        // approved content mapping wins and offer is dequeued
        var offerNoHashButMapped = offer(businessSupplier, "offerNoHashButMapped", CONTENT_PROCESSING, null, 1L)
            .setMarketSpecificContentHashSent(null)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100L, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        testChooseValidationCheckWithHigherLevelBody(offerGood, offerNoHashButMapped);
    }

    @Test
    @Override
    public void testErrorCardCreationNotAllowed() {
        var offerBad = offer(businessSupplier, "offerBad", INVALID, null, 1L)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(restrictedCategory.getCategoryId(), Offer.BindingKind.APPROVED);
        var offerGood = offer(businessSupplier, "offerGood", CONTENT_PROCESSING, null, 1L)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);
        testErrorCardCreationNotAllowed(offerBad, offerGood);
    }

    @Test
    public void testAgBatchLimit() {
        storageKeyValueService.putValue(BATCH_SIZE_KEY, 100);
        storageKeyValueService.putValue(AG_BATCH_LIMIT_KEY, 2);
        storageKeyValueService.invalidateCache();

        var offer1 = offer(businessSupplier, "offer1", NEED_CONTENT, null, 1L);
        var offer2 = offer(businessSupplier, "offer2", NEED_CONTENT, null, 1L);
        var offer3 = offer(businessSupplier, "offer3", NEED_CONTENT, null, 1L);
        offerRepository.insertOffers(offer1, offer2, offer3);

        configureDatacampResponse(Map.of(
            offer1, 1L,
            offer2, 1L,
            offer3, 1L
        ));
        configureAgResponses(0);

        queue.readAllWithoutGroupId(b -> assertThat(b).hasSize(3), 100, 100);

        service.sendFromQueue();

        // two batches of 2 and 1 offers
        verify(agServiceImpl, times(2)).startContentProcessing(any(), any());

        assertThat(queue.findAll()).hasSize(0);

        assertThatLogWasWritten(3);
    }

    @Test
    public void testForcedOffersIgnoreSomeFailures() {
        // Validation failure
        var offer1 = offer(businessSupplier, "offer1", IN_CLASSIFICATION, null, 1L);
        // Has content mapping
        var offer2 = offer(businessSupplier, "offer2", NEED_CONTENT, null, 1L)
            .updateApprovedSkuMapping(new Offer.Mapping(0, LocalDateTime.now()), Offer.MappingConfidence.CONTENT);
        // No content
        var offer3 = offer(businessSupplier, "offer3", NEED_CONTENT, null, 1L)
            .setMarketSpecificContentHash(null)
            .setMarketSpecificContentHashSent(null);

        // crutch: this offer is broken, because before fix configureDatacampResponse always set hashSent:
        offer3.setMarketSpecificContentHashSent(1L);

        offerRepository.insertOffers(offer1, offer2, offer3);
        // Insert by hand bc observer won't do it for invalid offer
        queue.insertOrUpdate(new ContentProcessingOffer(
            offer1.getBusinessId(), offer1.getShopSku(), null, Instant.now(), true, false
        ));
        queue.insertOrUpdate(new ContentProcessingOffer(
            offer3.getBusinessId(), offer3.getShopSku(), null, Instant.now(), true, false
        ));

        queue.updateBatch(queue.findAllByBusinessSkuKeys(List.of(offer1.getBusinessSkuKey(),
                offer2.getBusinessSkuKey(), offer3.getBusinessSkuKey()))
            .stream().map(it -> it.setForce(true))
            .collect(Collectors.toList()));

        configureDatacampResponse(Map.of(offer1, 1L, offer2, 1L, offer3, 1L));
        configureAgResponses(0);

        assertThat(queue.findAll()).hasSize(3);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        var argCaptor = ArgumentCaptor.forClass(OfferContentProcessingRequest.class);
        verify(agServiceImpl, times(1)).startContentProcessing(argCaptor.capture(), any());

        var allOffersIsForce = argCaptor.getValue().getOffersWithFlagsList()
            .stream()
            .allMatch(OfferContentProcessingRequest.OfferWithFlags::getForceSend);

        assertThat(argCaptor.getValue().getOffersWithFlagsList()).hasSize(3);
        assertThat(allOffersIsForce).isTrue();
        assertThat(result.getOffersFailed()).isEmpty();
        assertThat(result.getOffersDequeued()).hasSize(3);
        assertThat(result.getOffersSent()).hasSize(3);
        assertThatLogWasWritten(3);
    }

    @Test
    @Override
    public void testSuccessfullyProcessDeduplicated() {
        var offer1 = offer(businessSupplier, "offer1", NEED_CONTENT, null, 1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF);
        var offer2 = offer(businessSupplier, "offer2", AUTO_PROCESSED, null, 1L)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT)
            .setModelId(1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED);

        testSuccessfullyProcessDeduplicated(List.of(offer1, offer2), true);
    }

    @Test
    @Override
    public void testCorrectHandlingSecondaryOffers() {
        var offer1 = offer(businessSupplier, "offer1", NEED_CONTENT, null, 1L,
            DateTimeUtils.dateTimeNow().minus(1, ChronoUnit.DAYS))
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF);
        var offer2 = offer(businessSupplier, "offer2", AUTO_PROCESSED, null, 1L)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT)
            .setModelId(1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED);

        testCorrectHandlingSecondaryOffers(List.of(offer1, offer2), offer1);
    }

    @Test
    public void testSendDeduplicatedWithPskuNotPartnerSelf() {
        var offer = offer(businessSupplier, "offer2", PROCESSED, null, 1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.CONTENT);

        testSuccessfullyProcessDeduplicated(List.of(offer), true);
    }

    @Test
    public void testSendDeduplicatedDoNotSendMsku() {
        var offer1 = offer(businessSupplier, "offer1", PROCESSED, null, 1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.MARKET),
                Offer.MappingConfidence.CONTENT);
        var offer2 = offer(businessSupplier, "offer2", Offer.ProcessingStatus.OPEN, null, 1L);

        testSuccessfullyProcessDeduplicated(List.of(offer1, offer2), false);
    }

    @Test
    public void testShouldSkipFrozenModelsOffersUngrouped() {
        storageKeyValueService.putValue(AbstractContentProcessingSenderService.DISABLE_GG_CATEGORY_CHANGE, true);
        storageKeyValueService.invalidateCache();

        var frozenMsku = new Msku();
        frozenMsku.setMarketSkuId(1L)
            .setCategoryId(1L)
            .setParentModelId(111L)
            .setVendorId(1L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
        var simpleMsku1 = new Msku();
        simpleMsku1.setMarketSkuId(2L)
            .setCategoryId(2L)
            .setParentModelId(222L)
            .setVendorId(2L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
        var simpleMsku2 = new Msku();
        simpleMsku2.setMarketSkuId(3L)
            .setCategoryId(2L)
            .setParentModelId(333L)
            .setVendorId(2L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
        mskuRepository.save(frozenMsku, simpleMsku1, simpleMsku2);

        categoryService.addCategory(1L);
        categoryService.addCategory(2L);
        categoryService.addCategory(3L);

        var offerWithFrozenMsku = offer(businessSupplier, "offer1", NEED_CONTENT, null, 1L)
            .setId(1111)
            .setModelId(111L)
            .setCategoryIdInternal(1L)
            .setMappedCategoryId(1L)
            .setApprovedSkuMappingInternal(new Offer.Mapping(frozenMsku.getMarketSkuId(), LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE);

        var offerWithFrozenMskuAndDiffCategories = offer(businessSupplier, "offer2", NEED_CONTENT, null, 1L)
            .setId(2000)
            .setModelId(222L)
            .setCategoryIdInternal(2L)
            .setMappedCategoryId(2L)
            .setApprovedSkuMappingInternal(new Offer.Mapping(frozenMsku.getMarketSkuId(), LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE);

        var offerWithInconsistentCategories = offer(businessSupplier, "offer3", NEED_CONTENT, null, 1L)
            .setId(3000)
            .setModelId(333L)
            .setCategoryIdInternal(4L)
            .setMappedCategoryId(4L)
            .setApprovedSkuMappingInternal(new Offer.Mapping(simpleMsku2.getMarketSkuId(), LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE);

        var migrationModel1 = new MigrationModel()
            .setSourceOfferId(offerWithFrozenMsku.getId())
            .setModelId(offerWithFrozenMsku.getModelId())
            .setChangeSource(MigrationModelChangeSource.CLASSIFICATION)
            .setStatus(MigrationModelStatus.IN_PROCESS);
        var migrationModel2 = new MigrationModel()
            .setModelId(offerWithFrozenMskuAndDiffCategories.getModelId())
            .setSourceOfferId(offerWithFrozenMskuAndDiffCategories.getId())
            .setChangeSource(MigrationModelChangeSource.CLASSIFICATION)
            .setStatus(MigrationModelStatus.IN_PROCESS);
        migrationModelRepository.save(migrationModel1, migrationModel2);

        var offers = List.of(offerWithFrozenMsku, offerWithFrozenMskuAndDiffCategories,
            offerWithInconsistentCategories);
        testShouldSkipFrozenModelsOffers(offers);
    }
}
