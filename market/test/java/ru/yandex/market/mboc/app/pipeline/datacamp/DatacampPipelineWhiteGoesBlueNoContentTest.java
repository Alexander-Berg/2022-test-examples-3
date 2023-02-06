package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * Test scenarios without allowed content from Datacamp pipeline.
 */
public class DatacampPipelineWhiteGoesBlueNoContentTest extends BaseDatacampPipelineTest {

    @Before
    public void setUpDc() throws Exception {
        super.setUpDc();
        categoryCachingService
            .setAcceptContentFromWhiteShops(OfferTestUtils.TEST_CATEGORY_INFO_ID, false);

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.update(info);

        var blueServiceSupplier = supplierRepository.findById(OfferTestUtils.BLUE_SUPPLIER_ID_1)
            .setFulfillment(true);
        supplierRepository.update(blueServiceSupplier);

    }

    @Test
    public void processWhiteWithContentThenBecomesBlueManualAcceptance() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.update(info);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import white offer auto-accepted")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(
                testDCWhiteOffer()
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                    .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                    .updateAcceptanceStatusForTests(OfferTestUtils.WHITE_SUPPLIER_ID, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send state to DataCamp 1")
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

            .step("Import blue service offer, not auto-accepted, offer stays WHITE in ProcessingStatus.OPEN")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                    .withDisabledFlag(true).build()))
            .endStep()

            .step("Catman accepts offer, it becomes blue")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK,
                OfferTestUtils.blueSupplierUnderBiz1(), true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()))
            .endStep()

            .step("Send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }

    @Test
    public void processWhiteOfferNoContentNoMapping() {
        var dcOffer = initialOffer();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.update(info);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .ignoreDefaultCheck()
            .addChecks(List.of(
                assertNoOfferInRepo(),
                assertLastDCContentSystemStatus(),
                verifyContentSystemStatusNoDiff(dcOffer),
                assertLastDCServiceOffers(),
                assertLastDCBasicOffer()
            ))
            .expectedState(DCPipelineState.full(
                testDCWhiteOffer(),
                DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0L))
                    .build())
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullStatus -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .withDisabledFlag(false).build()))
            .endStep()


            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(Set.of(
                OfferTestUtils.WHITE_SUPPLIER_ID,
                OfferTestUtils.BLUE_SUPPLIER_ID_1),
                initialOffer())))
            .addCheck(verifyContentSystemStatusHandle(dcOffer, st ->
                st.setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
            ))
            .expectedState(scenario.previousValidState()
                .modifyOffer(nullOffer -> testDCWhiteOffer()
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                    .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                    .setServiceOffers(new ArrayList<>())
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.WHITE_SUPPLIER_ID))
                    .updateAcceptanceStatusForTests(OfferTestUtils.WHITE_SUPPLIER_ID, Offer.AcceptanceStatus.OK)
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                    .withDisabledFlag(true).build()))
            .endStep()

            .execute();
    }

    @Test
    public void processBlueOfferNoContentNoMappingGoesToClassification() {
        var dcOffer = initialOffer();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.update(info);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                    .withDisabledFlag(true).build()))
            .endStep()

            .step("Catman accepts offer")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK,
                OfferTestUtils.blueSupplierUnderBiz1(), true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }

    @Test
    public void processWhiteOfferNoContentCategoryMappingNoKnowledgeThenBecomesBlueThenKnowledgeAppearsG() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Category has no knowledge")
            .action(tmsImportCategoryKnowledge(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

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
            .expectedState(scenario.previousValidState()
                .modifyOffer(nullOffer -> testDCWhiteOffer()
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                    .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                    .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_MISSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_MISSING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .addCheck(verifyContentSystemStatusHandle(dcOffer, st -> st
                .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)
            ))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(state -> state
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)))
            .endStep()

            .step("Category knowledge appears")
            .action(tmsImportCategoryKnowledge(true))
            .addCheck(verifyContentSystemStatusHandle(dcOffer, st -> st
                .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
            ))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")))
            .endStep()

            .step("Send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .build())
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .build()))
            .endStep()

            .execute();
    }

    @Test
    public void processWhiteOfferNoContentCategoryMappingNoKnowledgeThenBecomesBlueThenKnowledgeAppears() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Category has bo knowledge")
            .action(tmsImportCategoryKnowledge(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

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
            .expectedState(scenario.previousValidState()
                .modifyOffer(nullOffer -> testDCWhiteOffer()
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                    .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                    .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_MISSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_MISSING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)))
            .endStep()

            .step("Category knowledge appears")
            .action(tmsImportCategoryKnowledge(true))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")))
            .endStep()

            .step("Send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .build())
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .build()))
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
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)))
            .endStep()

            .step("Send state to DataCamp 1")
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
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(st ->
                    st.setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
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
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setMappedModelId(MODEL_PARENT_ID)
                .setMappedModelConfidence(Offer.MappingConfidence.PARTNER)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierModelMappingId(MODEL_PARENT_ID)
                .setSupplierModelMappingStatus(Offer.MappingStatus.AUTO_ACCEPTED)))
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
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(st ->
                    st.setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
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

            .step("Import white service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)))
            .endStep()

            .step("Send state to DataCamp 1")
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
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)))
            .endStep()

            .execute();
    }
}
