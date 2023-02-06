package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * Test scenarios with allowed content from Datacamp pipeline.
 */
public class DatacampPipelineWhiteGoesBlueAllowContentTest extends BaseDatacampPipelineTest {

    @Before
    public void setup() {
        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.update(info);

        var blueServiceSupplier = supplierRepository.findById(OfferTestUtils.BLUE_SUPPLIER_ID_1)
            .setFulfillment(true);
        supplierRepository.update(blueServiceSupplier);
    }

    @Test
    public void processWhiteOfferNoContentNoMapping() {
        var dcOffer = initialOffer();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import white service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .ignoreDefaultCheck()
            .addChecks(List.of(
                assertNoOfferInRepo(),
                assertLastDCContentSystemStatus(),
                verifyContentSystemStatusNoDiff(dcOffer),
                assertLastDCServiceOffers(),
                assertLastDCBasicOffer()
            ))
            .expectedState(DCPipelineState.full(testDCWhiteOffer(),
                DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0))
                    .build())
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullStatus -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }

    @Test
    public void processBlueOfferNoContentNoMappingGoesToNeedContent() {
        Supplier supplier = OfferTestUtils.blueSupplierUnderBiz1();
        var dcOffer = initialOffer();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.update(info);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .addChecks(List.of(
                verifyNotEnqueued(ytOfferUploadQueueService), // Won't be enqueued, see OfferCriterias.mustBeExported
                verifyNotEnqueued(erpOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(supplier)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(
                scenario.previousValidState()
                    .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        //TODO: CPC state is invalid right now.
                        // Need to support white offers classification and add statuses to make it valid
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                    .modifyDatacampServiceOffer(supplier.getId(), nullValue -> OfferBuilder.create()
                        .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1,
                            SHOP_SKU_DCP)
                        .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .withDisabledFlag(true).build()))
            .endStep()

            .step("Catman accepts offer")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, supplier, true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(supplier.getId(), serviceOffer -> OfferBuilder.create(serviceOffer)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }


    @Test
    public void processWhiteOfferNoContentCategoryMappingThenBecomesBlue() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("enable auto-accaptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("Import white service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyNotEnqueued(erpOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setAllowModelCreateUpdate(false)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(state -> state
                    .setAllowModelCreateUpdate(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)))
            .endStep()

            .execute();
    }

    @Test
    public void processWhiteOfferNoContentModelMappingThenBecomesBlue() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.modelMapping(MODEL_PARENT_ID))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("enable auto-accaptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("Import white service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyNotEnqueued(erpOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setMappedCategoryId(null)
                .setMappedCategoryConfidence(null)
                .setModelId(MODEL_PARENT_ID)
                .setMappedModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setMappedModelConfidence(Offer.MappingConfidence.PARTNER)
                .setSupplierModelMappingId(MODEL_PARENT_ID)
                .setSupplierModelMappingStatus(Offer.MappingStatus.AUTO_ACCEPTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(false)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .build())
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(state -> state
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setAllowModelCreateUpdate(true)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)))
            .endStep()

            .execute();
    }

    @Test
    public void processWhiteOfferNoContentSkuMappingThenBecomesBlue() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.skuMapping(MARKET_SKU_ID_1))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("enable auto-accaptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()
            .step("Import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .addChecks(List.of(
                verifyNotEnqueued(ytOfferUploadQueueService),
                verifyNotEnqueued(erpOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setMappedCategoryId(null)
                .setMappedCategoryConfidence(null)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            ))
            .endStep()

            .step("Send datacamp states")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                    .incrementProcessingCounter()
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send datacamp states 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(state -> state
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setAllowModelCreateUpdate(true)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .withDisabledFlag(false).build())
            )
            .endStep()

            .execute();
    }

    @Test
    public void processTwoBlueOffersOneRejectedByCatman() {
        Supplier supplier1 = OfferTestUtils.blueSupplierUnderBiz1();
        Supplier supplier2 = OfferTestUtils.blueSupplierUnderBiz2();

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.update(info);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import blue service offer 1")
            .action(importOfferFromDC(toMessage(supplier1.getId(), initialOffer())))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(supplier1)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()

            .step("Import blue service offer 2")
            .action(importOfferFromDC(toMessage(supplier2.getId(), initialOffer())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer.addNewServiceOfferIfNotExistsForTests(supplier2)))
            .endStep()

            .step("Catman accepts service offer 1")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, supplier1, true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer.updateAcceptanceStatusForTests(supplier1.getId(), Offer.AcceptanceStatus.OK)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()))
            .endStep()

            .step("Catman accepts service offer 2")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, supplier2, true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer ->
                    offer.updateAcceptanceStatusForTests(supplier2.getId(), Offer.AcceptanceStatus.OK)
                ))
            .endStep()

            .step("Send datacamp states")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffers(nullMap -> Map.of(
                    supplier1.getId(), OfferBuilder.create()
                        .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, supplier1.getId(), SHOP_SKU_DCP)
                        .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                        .withDisabledFlag(false).build(),
                    supplier2.getId(), OfferBuilder.create()
                        .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, supplier2.getId(), SHOP_SKU_DCP)
                        .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                        .withDisabledFlag(false).build()
                    )
                ))
            .endStep()

            .step("Catman sends service offer 2 to TRASH")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.TRASH, supplier2, true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer.updateAcceptanceStatusForTests(supplier2.getId(), Offer.AcceptanceStatus.TRASH)))
            .endStep()

            .step("Send datacamp states 2")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(supplier2.getId(),
                    serviceOffer -> OfferBuilder.create(serviceOffer)
                        .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REJECTED)
                        .withDisabledFlag(true).build()))
            .endStep()
            .execute();
    }
}
