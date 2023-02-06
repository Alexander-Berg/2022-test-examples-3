package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.List;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.UltraControllerServiceData.UltraController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static ru.yandex.market.mboc.common.contentprocessing.config.ContentProcessingConfig.FAST_SKU_FEATURE_FLAG;

public class FastSkuPipelineTest extends BaseDatacampPipelineTest {

    @Before
    public void setupFast() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue(FAST_SKU_FEATURE_FLAG, true);
        modelStorageCachingService.addModel(model(FAST_SKU_ID, "Fast SKU", false, SimpleModel.ModelType.FAST_SKU));

        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.update(info);
    }

    /**
     * листовая разрешенная категория
     * создание БК
     * классификация - синие - маппинг на мскю
     **/
    @Test
    public void whenLeafAllowedCategoryThenParallelFastCreationAndOfferProcessing() {
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

            .step("make category leaf and allow fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
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
                .setProcessingCounter(1)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
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

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .setProcessingCounter(1)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .setProcessingCounter(2)))
            .endStep()

            .step("create blue logs ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator matched offer")
            .action(run(operatorMatchesOffer(MARKET_SKU_ID_1))
                .andThen(tmsProcessMatchingTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentSkuMapping(MAPPING_1)
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 3")
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

    /**
     * листовая разрешенная категория
     * создание БК --- удаление маппинга на БК
     * классификация - запрещенная категория --- синие - маппинг на мскю
     **/
    @Test
    public void whenCategoryChangesToProhibeitedThenFastSkuMappingIsRemoved() {
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

            .step("make category leaf and allow fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
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
                .setProcessingCounter(1)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
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

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .setProcessingCounter(1)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("add category with prohibited fast skus")
            .action(__ -> {
                categoryCachingService.addCategory(new Category()
                    .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1)
                    .setAcceptContentFromWhiteShops(false)
                    .setHasKnowledge(true)
                    .setAllowFastSkuCreation(false)
                    .setLeaf(true));
                updateCategoryKnowledgeInRepo(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1, true);
            })
            .ignoreDefaultCheck()
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator sets category with prohibited fast skus")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1, Offer.MappingConfidence.CONTENT)
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1, Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    // model is set to null because category has changed
                    .setModelId(null)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .incrementProcessingCounter()
                )
            )
            .endStep()

            .step("create blue logs ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator matched offer")
            .action(run(operatorMatchesOffer(MARKET_SKU_ID_1))
                .andThen(tmsProcessMatchingTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setContentSkuMapping(MAPPING_1)
                    .setModelId(MODEL_PARENT_ID)
                    .setCategoryIdInternal(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                )
            )
            .endStep()

            .step("send state to DataCamp 3")
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

    /**
     * листовая разрешенная категория
     * создание БК
     * классификация - УК находит СКЮ - ММ - маппинг на мскю
     **/
    @Test
    public void whenLeafAllowedCategoryThenParallelFastCreationAndOfferProcessing2() {
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

        DataCampOffer.Offer skutchedDcOffer = OfferBuilder.create(dcOffer)
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
                    .setMatchedId((int) MODEL_PARENT_ID)
                    .setSkutchType(UltraController.EnrichedOffer.SkutchType.BARCODE_SKUTCH);
            })
            .build();

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

            .step("make category leaf and allow fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setClassifierConfidenceInternal(0.1)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setProcessingCounter(1)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
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

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .setProcessingCounter(1)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("UC skutches offer on another SKU")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, skutchedDcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                    .setSuggestSkuMappingType(SkuBDApi.SkutchType.BARCODE_SKUTCH)
                    .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)))
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                    .setProcessingCounter(2)))
            .endStep()

            .step("operator accepts UC mapping")
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

            .step("send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(skutchedDcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                    .build()))
            .endStep()

            .execute();
    }

    /**
     * листовая запрещенная категория
     * БК не создается
     * классификация - синие - маппинг на мскю
     */
    @Test
    public void whenLeafProhibitedCategoryThenNoFastCreationAndOfferProcessing() {
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

            .step("make category leaf and prohibit fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(false)
                    .setLeaf(true)))
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
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
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

            .step("offer is not sent to AG")
            .action(__ -> {
            })
            .addCheck(verifyOfferIsNotInContentProcessingQueue(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .incrementProcessingCounter()))
            .endStep()

            .step("create blue logs ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator matched offer")
            .action(run(operatorMatchesOffer(MARKET_SKU_ID_1))
                .andThen(tmsProcessMatchingTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentSkuMapping(MAPPING_1)
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)))
            .endStep()

            .step("send state to DataCamp 3")
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

    /**
     * не листовая категория
     * БК не создается
     * классификация - синие - маппинг на мскю
     */
    @Test
    public void whenNotLeafCategoryThenNoFastCreationAndOfferProcessing() {
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

            .step("make category not leaf and allow fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(false)))
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
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
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

            .step("offer is not sent to AG")
            .action(__ -> {
            })
            .addCheck(verifyOfferIsNotInContentProcessingQueue(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .incrementProcessingCounter()))
            .endStep()

            .step("create blue logs ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator matched offer")
            .action(run(operatorMatchesOffer(MARKET_SKU_ID_1))
                .andThen(tmsProcessMatchingTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentSkuMapping(MAPPING_1)
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)))
            .endStep()

            .step("send state to DataCamp 3")
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

    /**
     * ММ - подтверждение саджеста
     * ---- БК не создается
     * ММ - маппинг на мскю
     */
    @Test
    public void whenSuccessfulMappingModerationThenNoFastCreationAndOfferProcessing() {
        DataCampOffer.Offer skutchedDcOffer = OfferBuilder.create(initialOffer())
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

        ultraControllerServiceMock.createMockResponse(List.of(skutchedDcOffer));

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

            .step("make category leaf and allow fast sku creation")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, skutchedDcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.NO_SKUTCH)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(skutchedDcOffer))
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

            .step("send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(skutchedDcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                    .build()))
            .endStep()

            .execute();
    }

    /**
     * ММ - NEED_INFO
     * ---- БК не создается в NEED_INFO
     * ММ - NEED_INFO
     */
    @Test
    public void whenNeedInfoMappingModerationThenParallelFastCreationAndOfferProcessing() {
        DataCampOffer.Offer skutchedDcOffer = OfferBuilder.create(initialOffer())
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

        ultraControllerServiceMock.createMockResponse(List.of(skutchedDcOffer));

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

            .step("make category leaf and allow fast sku creation")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, skutchedDcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.NO_SKUTCH)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(skutchedDcOffer))
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

            .step("operator sets need_info")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.NEED_INFO, MARKET_SKU_ID_1,
                ContentCommentType.INCORRECT_INFORMATION)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .setContentComments(new ContentComment(ContentCommentType.INCORRECT_INFORMATION))
                )
            )
            .endStep()

            .execute();
    }

    /**
     * ММ - отклонение саджеста
     * ------------- создание БК
     * ММ - reject - классификация
     */
    @Test
    public void whenRejectedMappingModerationThenParallelFastCreationAndOfferProcessing() {
        DataCampOffer.Offer skutchedDcOffer = OfferBuilder.create(initialOffer())
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

        ultraControllerServiceMock.createMockResponse(List.of(skutchedDcOffer));

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

            .step("make category leaf and allow fast sku creation")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
            .ignoreDefaultCheck()
            .expectedState(DCPipelineState.empty())
            .endStep()

            .step("import blue offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, skutchedDcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MARKET_SKU_ID_1, MARKET_SKU_TITLE_1))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.NO_SKUTCH)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(skutchedDcOffer))
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

            .step("operator rejects mapping")
            .action(run(operatorModeratesMapping(
                SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED, MARKET_SKU_ID_1)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .incrementProcessingCounter()
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(skutchedDcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(skutchedDcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .build()))
            .endStep()

            .execute();
    }

    /**
     * книжная неуверенная разрешенная категория
     * --------------- создание БК
     * классификация - синие - маппинг на мскю
     */
    @Test
    public void whenBookSuggestAllowedCategoryThenClassificationThenParallelFastCreationAndOfferProcessing() {
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

            .step("make category book leaf and allow fast skus")
            .action(__ -> {
                Mockito.when(booksService.isBookCategory(Mockito.any(Category.class)))
                    .thenReturn(true);
                categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .ifPresent(category -> category
                        .setAcceptContentFromWhiteShops(false)
                        .setAllowFastSkuCreation(true)
                        .setLeaf(true));
            })
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
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
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

            .step("offer is not sent to AG")
            .action(__ -> {
            })
            .addCheck(verifyOfferIsNotInContentProcessingQueue(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .setProcessingCounter(2)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .setProcessingCounter(2)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("create blue logs ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator matched offer")
            .action(run(operatorMatchesOffer(MARKET_SKU_ID_1))
                .andThen(tmsProcessMatchingTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentSkuMapping(MAPPING_1)
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 3")
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

    /**
     * книжная уверенная запрещенная категория
     * --------------- БК не создается
     * классификация - синие - маппинг на мскю
     */
    @Test
    public void whenBookSuggestProhibitedCategoryThenClassificationThenNoFastCreationAndOfferProcessing() {
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

            .step("make category book leaf and prohibit fast skus")
            .action(__ -> {
                Mockito.when(booksService.isBookCategory(Mockito.any(Category.class)))
                    .thenReturn(true);
                categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .ifPresent(category -> category
                        .setAcceptContentFromWhiteShops(false)
                        .setAllowFastSkuCreation(false)
                        .setLeaf(true));
            })
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
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
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

            .step("offer is not sent to AG")
            .action(__ -> {
            })
            .addCheck(verifyOfferIsNotInContentProcessingQueue(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator accepts suggested category")
            .action(run(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                    .incrementProcessingCounter()
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("offer is not sent to AG")
            .action(__ -> {
            })
            .addCheck(verifyOfferIsNotInContentProcessingQueue(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("create blue logs ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator matched offer")
            .action(run(operatorMatchesOffer(MARKET_SKU_ID_1))
                .andThen(tmsProcessMatchingTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentSkuMapping(MAPPING_1)
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
            .endStep()

            .step("send state to DataCamp 3")
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

    /**
     * листовая разрешенная категория
     * создание БК
     * классификация - NEED_CONTENT
     **/
    @Test
    public void whenLeafAllowedGoodCategoryThenParallelFastCreationAndNeedContent() {
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

            .step("make category good leaf and allow fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(true)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
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
                .setProcessingCounter(1)
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
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .setProcessingCounter(1)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(false)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
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
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)))
            .endStep()

            .step("send state to DataCamp 3")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(status -> status
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT))
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, so -> OfferBuilder.create(so)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT)
                    .build()))
            .endStep()

            .execute();
    }

    @Test
    public void whenFastCreatedAndOperatorReturnsNoKnowledgeThenFastMappingRemoved() {
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

            .step("make category leaf and allow fast skus")
            .action(__ -> categoryCachingService.getCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .ifPresent(category -> category
                    .setAcceptContentFromWhiteShops(false)
                    .setAllowFastSkuCreation(true)
                    .setLeaf(true)))
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
                .setProcessingCounter(1)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
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

            .step("offer is sent to AG")
            .action(state -> {
                makeContentProcessingOfferChangedDayAgo(OfferTestUtils.BIZ_ID_SUPPLIER, SHOP_SKU_DCP);
                putOfferToDatacampService(dcOffer);
                ungroupedContentProcessingSenderService.sendFromQueue();
                groupedContentProcessingSenderService.sendFromQueue();
            })
            .addCheck(verifyOfferSentToAg())
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)))
            .endStep()

            .step("receive mapping to fast sku from AG")
            .action(agSendsDCMapping(dcOffer, null, FAST_SKU_ID))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                    .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                    .setGutginSkuMapping(OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1))
                    .updateApprovedSkuMapping(
                        OfferTestUtils.mapping(FAST_SKU_ID, MARKET_SKU_TITLE_1),
                        Offer.MappingConfidence.PARTNER_FAST)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .setProcessingCounter(1)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)))
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("operator says NO_CATEGORY")
            .action(run(operatorClassifiesOfferByFile(ContentCommentType.NO_CATEGORY, ""))
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_CATEGORY)
                    .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION)
                    .setContentComments(new ContentComment(ContentCommentType.NO_CATEGORY))
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                )
            )
            .endStep()

            .execute();
    }
}
