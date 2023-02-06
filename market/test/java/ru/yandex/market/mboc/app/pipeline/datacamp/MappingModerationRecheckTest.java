package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.Collections;

import Market.DataCamp.DataCampOfferMapping;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;

import static java.time.Instant.now;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.CONTENT_PROCESSING;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_PROCESS;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_RECHECK_MODERATION;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.PROCESSED;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingSource.PARTNER;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingStatus.NEED_RECHECK;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingStatus.ON_RECHECK;
import static ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService.DELETED_MODEL;
import static ru.yandex.market.mboc.common.services.offers.mapping.SaveMappingModerationService.ROLLBACK_ENABLED_FLAG;
import static ru.yandex.market.mboc.common.services.proto.BaseOfferProcessesService.ALLOW_RECHECK_MAPPING;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

public class MappingModerationRecheckTest extends BaseDatacampPipelineTest {

    @Before
    public void setUpMappingModerationRecheck() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue(ROLLBACK_ENABLED_FLAG, true);
        storageKeyValueService.putValue(ALLOW_RECHECK_MAPPING, true);

        var info = categoryInfoRepository.findById(TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.update(info);

        modelStorageCachingService.addModel(model(FAST_SKU_ID, "Fast SKU", false, SimpleModel.ModelType.FAST_SKU));

        categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .ifPresent(category -> category
                .setAcceptContentFromWhiteShops(false)
                .setAllowFastSkuCreation(true)
                .setLeaf(true));
    }

    @Test
    public void testRejectFirstThenCreateFastSkuThenRejectSecond()  {
        var offerInRepo = testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
        offerRepository.insertOffers(offerInRepo);

        // generate mappings history
        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(offerInRepo);
        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(offerInRepo);

        var dcOfferBuilder = OfferBuilder.create(initialOffer()).get();
        dcOfferBuilder.getContentBuilder()
            .getBindingBuilder()
            .getPartnerMappingModerationBuilder()
            .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                .setMarketSkuId(MAPPING_2.getMappingId())
                .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY));
        var dcOffer = dcOfferBuilder.build();

        var scenario = createScenario();
        scenario
            .step("import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(offerInRepo.copy()
                .setPartnerMappingModerationDecision(Offer.PartnerMappingModerationDecision.DENY)
                .setRecheckSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET))
                .setRecheckMappingSource(PARTNER)
                .setRecheckMappingStatus(ON_RECHECK)
                .updateProcessingStatusIfValid(IN_RECHECK_MODERATION)
                .setProcessingCounter(1)))
            .endStep()
            .step("operator rejects mapping 2")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED,
                MAPPING_2.getMappingId())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setPartnerMappingModerationDecision(null)
                    .setMappingDestination(Offer.MappingDestination.WHITE)
                    .updateApprovedSkuMapping(Offer.Mapping.fromSku(DELETED_MODEL))
                    .setApprovedSkuMappingConfidence(Offer.MappingConfidence.RESET)
                    .setSmLastExecutionTs(now())
                    .setRecheckSkuMapping(MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET))
                    .setRecheckMappingSource(Offer.RecheckMappingSource.OPERATOR)
                    .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_ROLLBACK)
                    .setLastPrimaryProcessingStatus(IN_RECHECK_MODERATION)
                    .updateProcessingStatusIfValid(IN_RECHECK_MODERATION)
                    .setProcessingCounter(2)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                ))
            .endStep()

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setModelId(MODEL_PARENT_ID)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST
                    )
                    .setProcessingCounter(3)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("operator rejects mapping 1")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED,
                MAPPING_1.getMappingId())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(IN_PROCESS)
                    .setProcessingCounter(4)
                    .setRecheckMappingStatus(null)
                    .setRecheckMappingSource(null)
                    .setRecheckSkuMapping(null)))
            .endStep()

            .execute();
    }

    @Test
    public void testRejectFirstThenCreateFastSkuThenAcceptSecond() {
        var offerInRepo = testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
        offerRepository.insertOffers(offerInRepo);

        // generate mappings history
        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(offerInRepo);
        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(offerInRepo);

        var dcOfferBuilder = OfferBuilder.create(initialOffer()).get();
        dcOfferBuilder.getContentBuilder()
            .getBindingBuilder()
            .getPartnerMappingModerationBuilder()
            .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                .setMarketSkuId(MAPPING_2.getMappingId())
                .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY));
        var dcOffer = dcOfferBuilder.build();

        var scenario = createScenario();
        scenario
            .step("import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(offerInRepo.copy()
                .setRecheckSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET))
                .setRecheckMappingSource(PARTNER)
                .setRecheckMappingStatus(ON_RECHECK)
                .setPartnerMappingModerationDecision(Offer.PartnerMappingModerationDecision.DENY)
                .updateProcessingStatusIfValid(IN_RECHECK_MODERATION)
                .setProcessingCounter(1)))
            .endStep()

            .step("operator rejects mapping 2")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED,
                MAPPING_2.getMappingId())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setPartnerMappingModerationDecision(null)
                    .setMappingDestination(Offer.MappingDestination.WHITE)
                    .updateApprovedSkuMapping(Offer.Mapping.fromSku(DELETED_MODEL))
                    .setApprovedSkuMappingConfidence(Offer.MappingConfidence.RESET)
                    .setSmLastExecutionTs(now())
                    .setRecheckSkuMapping(MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET))
                    .setRecheckMappingSource(Offer.RecheckMappingSource.OPERATOR)
                    .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_ROLLBACK)
                    .setLastPrimaryProcessingStatus(IN_RECHECK_MODERATION)
                    .updateProcessingStatusIfValid(IN_RECHECK_MODERATION)
                    .setProcessingCounter(2)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                ))
            .endStep()

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setModelId(MODEL_PARENT_ID)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST
                    )
                    .setProcessingCounter(3)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("operator accepts mapping 1")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.ACCEPTED,
                MAPPING_1.getMappingId())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateApprovedSkuMapping(MAPPING_1, Offer.MappingConfidence.CONTENT)
                    .updateProcessingStatusIfValid(PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .setContentSkuMapping(MAPPING_1)
                    .setRecheckMappingStatus(Offer.RecheckMappingStatus.MAPPING_CONFIRMED)))
            .endStep()

            .execute();
    }

    @Test
    public void whenSupplierDenyConfirmedMappingThenOk() {
        //setup
        var offerInRepo = testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
        offerRepository.insertOffers(offerInRepo);

        // generate mappings history
        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(offerInRepo);
        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(offerInRepo);

        var dcOfferBuilder = OfferBuilder.create(initialOffer()).get();
        dcOfferBuilder.getContentBuilder()
            .getBindingBuilder()
            .getPartnerMappingModerationBuilder()
            .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                .setMarketSkuId(MAPPING_2.getMappingId())
                .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY));

        var dcOffer = dcOfferBuilder.build();
        var datacampMessage = toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer);

        var expectedOffer = offerInRepo.copy()
            .setRecheckSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET))
            .setRecheckMappingSource(PARTNER)
            .setRecheckMappingStatus(ON_RECHECK)
            .setContentProcessed(false)
            .setTicketCritical(false)
            .setPartnerMappingModerationDecision(Offer.PartnerMappingModerationDecision.DENY)
            .updateProcessingStatusIfValid(IN_RECHECK_MODERATION)
            .setProcessingCounter(1);


        // import offer as usual
        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(datacampMessage));

        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());

        assertEquals(offerInRepo, expectedOffer);

        //operator confirmed mapping
        SupplierOffer.MappingModerationTaskResult.Builder results =
            SupplierOffer.MappingModerationTaskResult.newBuilder()
                .setStatus(SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.ACCEPTED)
                .setOfferId(Long.toString(offerInRepo.getId()))
                .setMarketSkuId(MAPPING_2.getMappingId())
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setShopSkuId(SHOP_SKU)
                .setOfferId(String.valueOf(offerInRepo.getId()))
                .setStaffLogin(YANG_OPERATOR_LOGIN);

        MboCategory.SaveMappingModerationResponse response = mboCategoryService.saveMappingsModeration(
            MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(results)
                .build());

        Assertions.assertThat(response.getResult().getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        expectedOffer = offerInRepo.copy()
            .updateApprovedSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT)
            .updateProcessingStatusIfValid(PROCESSED)
            .setLastPrimaryProcessingStatus(IN_RECHECK_MODERATION)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
            .setContentSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET))
            .setMappingModifiedBy(YANG_OPERATOR_LOGIN)
            .setModelId(1L)
            .setVendorId(100500)
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);

        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());

        assertEquals(offerInRepo, expectedOffer);


        //partner deny mapping

        var dcDenyOfferBuilder = OfferBuilder.create(initialOffer()).get();
        dcDenyOfferBuilder.getContentBuilder()
            .getBindingBuilder()
            .getPartnerMappingModerationBuilder()
            .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                .setMarketSkuId(MAPPING_2.getMappingId())
                .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY));

        var dcDenyOffer = dcDenyOfferBuilder.build();
        var datacampDenyMessage = toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer);

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(datacampDenyMessage));

        expectedOffer = offerInRepo.copy()
            .updateApprovedSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT)
            .updateProcessingStatusIfValid(PROCESSED)
            .setLastPrimaryProcessingStatus(IN_RECHECK_MODERATION)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
            .setContentSkuMapping(MAPPING_2.copyWithSkuType(Offer.SkuType.MARKET))
            .setPartnerMappingModerationDecision(Offer.PartnerMappingModerationDecision.DENY)
            .setMappingModifiedBy(YANG_OPERATOR_LOGIN)
            .setModelId(1L)
            .setVendorId(100500)
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);

        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());

        assertEquals(offerInRepo, expectedOffer);
    }

    private void assertEquals(Offer offer, Offer other) {
        Assertions.assertThat(offer).usingRecursiveComparison()
            .ignoringFields("lastVersion",
                "marketSpecificContentHash",
                "marketSpecificContentHashSent",
                "offerContent.sourcePicUrls",
                "offerContent.unitedSize",
                "updated",
                "processingStatusModified",
                "approvedSkuMapping.timestamp",
                "contentSkuMapping.timestamp")
            .isEqualTo(other);
    }


    @Test
    public void testSendToRecheckAfterContentProcessing() {
        var offerInRepo = testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);
        offerRepository.insertOffers(offerInRepo);

        offerInRepo = offerRepository.getOfferById(offerInRepo.getId());
        offerInRepo.updateApprovedSkuMapping(
            MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET), Offer.MappingConfidence.PARTNER_SELF);
        offerRepository.updateOffers(offerInRepo);

        var dcOfferBuilder = OfferBuilder.create(initialOffer()).get();
        dcOfferBuilder.getContentBuilder()
            .getBindingBuilder()
            .getPartnerMappingModerationBuilder()
            .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                .setMarketSkuId(MAPPING_1.getMappingId())
                .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY));
        var dcOffer = dcOfferBuilder.build();

        var scenario = createScenario();
        scenario
            .step("import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(offerInRepo.copy()
                .setRecheckSkuMapping(MAPPING_1.copyWithSkuType(Offer.SkuType.MARKET))
                .setRecheckMappingSource(PARTNER)
                .setRecheckMappingStatus(NEED_RECHECK)
                .setPartnerMappingModerationDecision(Offer.PartnerMappingModerationDecision.DENY)
                // CONTENT_PROCESSING status does not change
                .updateProcessingStatusIfValid(CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive same mapping from AG")
            .action(agSendsDCMapping(dcOffer, MODEL_PARENT_ID, MAPPING_1.getMappingId()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setModelId(MODEL_PARENT_ID)
                    .setGutginSkuMapping(offer.getApprovedSkuMapping())
                    // content has been processed, move to recheck MM
                    .setRecheckMappingStatus(ON_RECHECK)
                    .updateProcessingStatusIfValid(IN_RECHECK_MODERATION)
                    .setProcessingCounter(1)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .execute();
    }
}
