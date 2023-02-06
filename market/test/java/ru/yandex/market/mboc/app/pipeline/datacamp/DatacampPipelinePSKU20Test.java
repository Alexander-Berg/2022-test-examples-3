package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.time.LocalDate;
import java.util.List;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.UltraControllerServiceData.UltraController;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator.ENABLED_TAGS;
import static ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo.CategoryTag.FASHION;
import static ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo.CategoryTag.SIZED_FASHION;
import static ru.yandex.market.mboc.common.services.offers.ReopenNeedInfoService.SEND_IF_NO_ACTIVITY;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.dsbsSupplierUnderBiz;

public class DatacampPipelinePSKU20Test extends BaseDatacampPipelineTest {

    private static final String VALIDATION_TEXT = "Validation error";
    private static final String CODE = "market.ir";

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
    public void testUnconfidentlyClassifiedOffer() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(
                OfferBuilder.categoryMapping(
                    OfferTestUtils.TEST_CATEGORY_INFO_ID, OfferTestUtils.DEFAULT_CATEGORY_NAME))
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.1)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN))
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp")
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

            .execute();
    }

    @Test
    public void testAutoClassifiedOffer() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(
                OfferBuilder.categoryMapping(
                    OfferTestUtils.TEST_CATEGORY_INFO_ID, OfferTestUtils.DEFAULT_CATEGORY_NAME))
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN))
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.99)
                .setAutomaticClassification(true)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
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

            .execute();
    }

    @Test
    public void testProcessOfferFromNeedInfoToReopenWithFilledGroupId() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.skuMapping(MARKET_SKU_ID_1))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        categoryInfoRepository.update(categoryInfoRepository.findById(TEST_CATEGORY_INFO_ID).setTags(
            List.of(FASHION, SIZED_FASHION)
        ));
        //restrict convert dsbs-fashion to blue.
        storageKeyValueService.putValue(ENABLED_TAGS, "{\"FASHION\":\"" + LocalDate.now().plusDays(5) + "\"}");
        storageKeyValueService.invalidateCache();

        DataCampOffer.Offer.Builder builder = dcOffer.toBuilder();
        builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().setGroupId(
            DataCampOfferMeta.Ui32Value.newBuilder().setValue(305).build()
        );

        Offer offer = testDCOffer(dsbsSupplierUnderBiz());
        offer
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setModelId(MODEL_PARENT_ID)
            .setMappedModelId(MODEL_PARENT_ID)
            .setMappedModelConfidence(Offer.MappingConfidence.PARTNER)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setMappedCategoryId(null)
            .setMappedCategoryConfidence(null)
            .setSupplierCategoryId(TEST_CATEGORY_INFO_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
            .setMappedCategoryId(TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierModelMappingId(MODEL_PARENT_ID)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        offerRepository.insertOffers(offer);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Try classify offer")
            .action(tmsCreateTrackerTickets())
            .expectedState(DCPipelineState.onlyOffer(offer
                .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(offer.getShopSku()))
                .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_INFO)
                .setContentComments(List.of(new ContentComment(ContentCommentType.NEED_CLASSIFICATION_INFORMATION,
                    "не группированный товар")))))
            .endStep()

            .step("Import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.DSBS_SUPPLIER_ID, builder.build())))
            .expectedState(scenario.previousValidState().modifyOffer(o -> o.setMappingDestination(Offer.MappingDestination.DSBS)
                .setOfferDestination(Offer.MappingDestination.DSBS)
                .setSupplierSkuMapping(MAPPING_1)
                .setGroupId(305)))
            .endStep()

            .step("update timestamp")
            .action(task(() -> {
                var o = offerRepository.getOfferById(offer.getId());
                o.setProcessingStatusModifiedInternal(o.getProcessingStatusModified().minus(SEND_IF_NO_ACTIVITY))
                    .setContentChangedTs(o.getContentChangedTs().minus(SEND_IF_NO_ACTIVITY));
                offerRepository.updateOffers(o);
            }))
            .expectedState(scenario.previousValidState().modifyOffer(o -> o.setMappingDestination(Offer.MappingDestination.DSBS)
                .setOfferDestination(Offer.MappingDestination.DSBS)
                .setSupplierSkuMapping(MAPPING_1)
                .setGroupId(305)))
            .endStep()

            .step("offer went out from NEED_INFO")
            .action(tmsReopenNeedInfoOffers())
            .expectedState(scenario.previousValidState().modifyOffer(prevOffer ->
                prevOffer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
                    .setContentComments(List.of())
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.REOPEN)
                    .setProcessingCounter(2)))
            .endStep()
            .execute();
    }

    @Test
    public void testManuallyClassifiedMatchedOffer() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(
                OfferBuilder.modelMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID, MODEL_PARENT_ID,
                    OfferTestUtils.DEFAULT_CATEGORY_NAME, MODEL_PARENT_TITLE))
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.1)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)
                    .setMatchedId((int) MODEL_PARENT_ID))
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("enable auto-acceptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter()))
            .endStep()

            .step("send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(false)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .addCheck((desc, state) -> {
                var offer = state.getOffer();
                Assertions.assertThat(contentProcessingQueueRepository
                        .findAllByBusinessSkuKeys(offer.getBusinessSkuKey()))
                    .hasSize(1);
            })
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .build())
                .modifyDatacampStatus(state -> state
                    .setAllowModelCreateUpdate(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)))
            .endStep()

            .execute();
    }

    @Test
    public void testMatchedAutoClassifiedOfferSentToContentProcessing() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(
                OfferBuilder.modelMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID, MODEL_PARENT_ID,
                    OfferTestUtils.DEFAULT_CATEGORY_NAME, MODEL_PARENT_TITLE))
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)
                    .setMatchedId((int) MODEL_PARENT_ID))
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("enable auto-acceptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.99)
                .setAutomaticClassification(true)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(false)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }

    @Test
    public void testSkuchedOfferOperatorAcceptsMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(OfferBuilder.mapping(
                OfferTestUtils.TEST_CATEGORY_INFO_ID, MODEL_PARENT_ID, MARKET_SKU_ID_1,
                OfferTestUtils.DEFAULT_CATEGORY_NAME, MODEL_PARENT_TITLE, MARKET_SKU_TITLE_1))
            .withDefaultMarketContent(market -> {
                market.setMarketSkuPublishedOnMarket(true)
                    .setMarketSkuPublishedOnBlueMarket(true);
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.1)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)
                    .setMatchedId((int) MODEL_PARENT_ID);
            })
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("enable auto-acceptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.1)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.NO_SKUTCH)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)))
            .endStep()

            .step("send state to DataCamp 1")
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

            .step("import blue part")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                    .incrementProcessingCounter()
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(state -> state
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .setAllowModelCreateUpdate(false)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                )
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                    .withDisabledFlag(false)
                    .build()
                )
            )
            .endStep()

            .step("operator accepts mapping")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.ACCEPTED,
                MARKET_SKU_ID_1)))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setMappingDestination(Offer.MappingDestination.BLUE)
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.CONTENT)
                    .setContentSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 4")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                    .build()))
            .endStep()

            .execute();
    }

    @Test
    public void testSkuchedOfferOperatorResetsStatusAndSendsToTrash() {
        Supplier blueSupplier = supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1);

        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(OfferBuilder.mapping(
                OfferTestUtils.TEST_CATEGORY_INFO_ID, MODEL_PARENT_ID, MARKET_SKU_ID_1,
                OfferTestUtils.DEFAULT_CATEGORY_NAME, MODEL_PARENT_TITLE, MARKET_SKU_TITLE_1))
            .withDefaultMarketContent(market -> {
                market.setMarketSkuPublishedOnMarket(true)
                    .setMarketSkuPublishedOnBlueMarket(true);
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.1)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)
                    .setMatchedId((int) MODEL_PARENT_ID);
            })
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("enable auto-acceptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplier)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.1)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.NO_SKUTCH)
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .incrementProcessingCounter()))
            .endStep()

            .step("send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(false)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("Catman resets offer status")
            .action(run(catmanResetsOfferStatus(OfferTestUtils.BLUE_SUPPLIER_ID_1)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)))
            .endStep()

            .step("Catman sends service offer to TRASH")
            .action(run(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.TRASH, blueSupplier, true)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.TRASH)))
            .endStep()

            .step("send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, serviceOffer ->
                    OfferBuilder.create(serviceOffer)
                        .withDisabledFlag(true)
                        .build())
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REJECTED))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REJECTED)
                    .build()))
            .endStep()

            .step("import blue offer again, nothing changes")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState())
            .endStep()

            .execute();
    }

    @Test
    public void testSkuchedAutoclassifiedOfferOperatorRejectsMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(OfferBuilder.mapping(
                OfferTestUtils.TEST_CATEGORY_INFO_ID, MODEL_PARENT_ID, MARKET_SKU_ID_1,
                OfferTestUtils.DEFAULT_CATEGORY_NAME, MODEL_PARENT_TITLE, MARKET_SKU_TITLE_1))
            .withDefaultMarketContent(market -> {
                market.setMarketSkuPublishedOnMarket(true)
                    .setMarketSkuPublishedOnBlueMarket(true);
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)
                    .setMatchedId((int) MODEL_PARENT_ID);
            })
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("enable auto-acceptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.99)
                .setAutomaticClassification(true)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.NO_SKUTCH)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)))
            .endStep()

            .step("send state to DataCamp 1")
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

            .step("import blue part")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                    .incrementProcessingCounter()
                    .addNewServiceOfferIfNotExistsForTests(supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)))
            .endStep()

            .step("send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
                .modifyDatacampStatus(state -> state
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)))
            .endStep()

            .step("operator rejects mapping")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED,
                MARKET_SKU_ID_1)))
            .addCheck((desc, state) -> {
                var offer = state.getOffer();
                Assertions.assertThat(contentProcessingQueueRepository
                        .findAllByBusinessSkuKeys(offer.getBusinessSkuKey()))
                    .hasSize(1);
            })
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("send state to DataCamp 4")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setAllowModelCreateUpdate(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .build())
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .build()))
            .endStep()

            .execute();
    }

    @Test
    public void testNoKnowledgeContentNotAllowed() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .withDefaultMarketSpecificContent()
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("prohibit content")
            .action(task(() -> categoryCachingService
                .setAcceptContentFromWhiteShops(OfferTestUtils.TEST_CATEGORY_INFO_ID, false)))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import category no knowledge")
            .action(tmsImportCategoryKnowledge(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import white offer with category mapping")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)))
            .endStep()

            .step("send state to DataCamp 1")
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

            .step("import category knowledge appears")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")))
            .endStep()

            .step("send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .build()))
            .endStep()

            .execute();
    }

    @Test
    public void testNoKnowledgeContentAllowed() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .withDefaultMarketSpecificContent()
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category no knowledge")
            .action(tmsImportCategoryKnowledge(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import white offer with category mapping")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)))
            .endStep()

            .step("send state to DataCamp 1")
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

            .step("import category knowledge appears")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .setContentComment("Знания появились, можно отправить на заведение")
                )
                .modifyDatacampStatus(status -> status
                    .setAllowModelCreateUpdate(false)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .build()))
            .endStep()

            .execute();
    }

    @Test
    public void testHasKnowledgeContentNotAllowed() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .withDefaultMarketSpecificContent()
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("prohibit content")
            .action(task(() -> categoryCachingService
                .setAcceptContentFromWhiteShops(OfferTestUtils.TEST_CATEGORY_INFO_ID, false)))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import white offer with category mapping")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUPPLIER)
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
                .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)))
            .endStep()

            .step("send state to DataCamp")
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

            .execute();
    }

    @Test
    public void testSupplierSendsUpdateWhileInContentProcessing() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withUcMapping(
                OfferBuilder.categoryMapping(
                    OfferTestUtils.TEST_CATEGORY_INFO_ID, OfferTestUtils.DEFAULT_CATEGORY_NAME))
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN))
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("import category knowledge")
            .action(tmsImportCategoryKnowledge(true))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("enable auto-accaptance in category")
            .action(updateCategoryManualAcceptanceInRepo(false))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.99)
                .setAutomaticClassification(true)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("send state to DataCamp")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("reimport blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            // offer stays in CONTENT_PROCESSING
            .expectedState(scenario.previousValidState())
            .endStep()

            .execute();
    }
}
