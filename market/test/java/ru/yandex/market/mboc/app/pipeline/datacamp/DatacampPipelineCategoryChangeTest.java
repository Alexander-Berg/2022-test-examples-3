package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.UltraControllerServiceData.UltraController;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class DatacampPipelineCategoryChangeTest extends BaseDatacampPipelineTest {

    public static final long SECOND_CATEGORY_ID = OfferTestUtils.TEST_CATEGORY_INFO_ID + 1;

    @Before
    public void setUpDc() throws Exception {
        super.setUpDc();
        Category secondCategory = new Category().setCategoryId(SECOND_CATEGORY_ID)
            .setAcceptGoodContent(true)
            .setAcceptContentFromWhiteShops(true)
            .setHasKnowledge(true)
            .setAcceptPartnerSkus(true);
        categoryCachingService.addCategories(secondCategory);

        updateCategoryKnowledgeInRepo(SECOND_CATEGORY_ID, true);
    }

    @Test
    public void testPartnerChangesCategoryForAutoClassifiedOfferAndIsRejected() {
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
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
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

            .step("supplier tries to change category to `second` and it's rejected")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID,
                OfferBuilder.create(dcOffer)
                    .withPartnerMapping(
                        OfferBuilder.categoryMapping(SECOND_CATEGORY_ID))
                    .get().build())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setSupplierCategoryId(SECOND_CATEGORY_ID)
                    .setSupplierCategoryMappingStatus(Offer.MappingStatus.REJECTED)
                    .setContentStatusActiveError(new ErrorInfo("mboc.error.category-mapping" +
                        ".category-restricted-to-single",
                        "Для оффера '{{offerId}}' разрешена только категория '{{allowedCategoryId}}'.",
                        ErrorInfo.Level.ERROR, Map.of("offerId", SHOP_SKU_DCP, "allowedCategoryId", 91497)))))
            .endStep()

            .execute();
    }

    @Test
    public void testPartnerChangesCategoryForMatchedOfferAndIsRejected() {
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

            .step("import white offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setModelId(MODEL_PARENT_ID)
                .setMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestModelMappingId(MODEL_PARENT_ID)
                .setSuggestMarketModelName(MODEL_PARENT_TITLE)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)))
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
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                    .setAllowModelCreateUpdate(false)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                    .withDisabledFlag(false).build()))
            .endStep()

            .step("supplier tries to change category to `second` and it's rejected")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID,
                OfferBuilder.create(dcOffer)
                    .withPartnerMapping(
                        OfferBuilder.categoryMapping(SECOND_CATEGORY_ID))
                    .get().build())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setSupplierCategoryId(SECOND_CATEGORY_ID)
                    .setContentStatusActiveError(
                        MbocErrors.get().cantMapCategorySinceOfferHasModelOrSku(offer.getShopSku()))
                    .setSupplierCategoryMappingStatus(Offer.MappingStatus.REJECTED)))
            .endStep()

            .execute();
    }

    @Test
    public void testPartnerChangesCategoryForSkuchedOfferAndIsRejected() {
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

            .step("supplier tries to change category to `second` and it's rejected")
            .action(importOfferFromDC(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID,
                OfferBuilder.create(dcOffer)
                    .withPartnerMapping(
                        OfferBuilder.categoryMapping(SECOND_CATEGORY_ID))
                    .get().build())))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .setSupplierCategoryId(SECOND_CATEGORY_ID)
                    .setContentStatusActiveError(
                        MbocErrors.get().cantMapCategorySinceOfferHasModelOrSku(offer.getShopSku()))
                    .setSupplierCategoryMappingStatus(Offer.MappingStatus.REJECTED)))
            .endStep()

            .execute();
    }
}
