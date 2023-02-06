package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.time.LocalDateTime;
import java.util.List;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampValidationResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.context.ApprovedSkuMappingContext;
import ru.yandex.market.mboc.common.services.offers.mapping.context.SkuMappingContext;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static Market.DataCamp.DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY;
import static Market.DataCamp.DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.blueSupplierUnderBiz;

/**
 * @author apluhin
 * @created 1/25/22
 */
public class AutoApproveTest extends BaseDatacampPipelineTest {

    @Before
    public void setup() {
        var info = categoryInfoRepository.findById(TEST_CATEGORY_INFO_ID);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.update(info);

        var blueServiceSupplier = supplierRepository.findById(OfferTestUtils.BLUE_SUPPLIER_ID_1)
            .setFulfillment(true);
        supplierRepository.update(blueServiceSupplier);
    }

    @Test
    public void testVerifyAutoApproveSupplierMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.skuMapping(MARKET_SKU_ID_1))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        DataCampOffer.Offer.Builder builder = dcOffer.toBuilder();
        builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().setCardSource(
            DataCampOfferContent.CardSource.newBuilder().setCardByMskuSearch(true).setMarketSkuId(MARKET_SKU_ID_1).build()
        );

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplierUnderBiz(OfferTestUtils.BLUE_SUPPLIER_ID_1))
                .setOfferDestination(Offer.MappingDestination.BLUE)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .incrementProcessingCounter()
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setMappedCategoryId(null)
                .setMappedCategoryConfidence(null)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
                .setSuggestCategoryMappingId(TEST_CATEGORY_INFO_ID)
            ))
            .endStep()

            .step("Send datacamp states")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusHandle(builder.build(), (st) -> st
                .setAllowCategorySelection(false)
                .setAllowModelSelection(false)
                .setAllowModelCreateUpdate(false)
                .setCpcState(CPC_CONTENT_READY)
                .setCpaState(CONTENT_STATE_READY)
            ))
            .expectedState(scenario.previousValidState()
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
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
            .step("Check same state offer, after verifyContentSystemStatusHandle")
            .action(__ -> {
            })
            .expectedState(scenario.previousValidState())
            .endStep()
            .execute();

        var lastSentDCOffer = getLastSentDCOffer(DataCampOfferUtil.extractExternalBusinessSkuKey(dcOffer));
        var basic = lastSentDCOffer.get().getBasic();
        var recommendations = unwrapAcceptanceRecommendations(basic.getResolution().getBySourceList());
        Assertions.assertThat(recommendations.get(DataCampValidationResult.RecommendationStatus.MANUAL)).hasSize(4);
        Assertions.assertThat(recommendations.get(DataCampValidationResult.RecommendationStatus.FINE)).hasSize(1);
    }

    @Test
    public void testAutoApproveSupplierMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.skuMapping(MARKET_SKU_ID_1))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        DataCampOffer.Offer.Builder builder = dcOffer.toBuilder();
        builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().setCardSource(
            DataCampOfferContent.CardSource.newBuilder().setCardByMskuSearch(true).setMarketSkuId(MARKET_SKU_ID_1).build()
        );

        Offer offer = testDCOffer(blueSupplierUnderBiz(OfferTestUtils.BLUE_SUPPLIER_ID_1));
        offer
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(MODEL_PARENT_ID)
            .setMappedModelId(MODEL_PARENT_ID)
            .setMappedModelConfidence(Offer.MappingConfidence.PARTNER)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setMappedCategoryId(null)
            .setMappedCategoryConfidence(null)
            .setSupplierCategoryId(TEST_CATEGORY_INFO_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
            .setMappedCategoryId(TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
            .setSupplierSkuMapping(MAPPING_1)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierModelMappingId(MODEL_PARENT_ID)
            .setSupplierModelMappingStatus(Offer.MappingStatus.AUTO_ACCEPTED)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        offerRepository.insertOffers(offer);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, builder.build())))
            .expectedState(DCPipelineState.onlyOffer(offer
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED)
                .setApprovedSkuMappingInternal(MAPPING_1)
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setAutoSkuMapping(MAPPING_1)
                .setSupplierModelMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setAutoApprovedMappingSource(Offer.AutoApprovedMappingSource.AUTO_APPROVE_FROM_SEARCH)
                .setAutoApprovedMapping(true)
                .setDatacampSkuIdFromSearch(MARKET_SKU_ID_1)
                .setDatacampSkuIdFromSearchStatus(Offer.MappingStatus.ACCEPTED)
                .setSupplierSkuMappingCheckTs(LocalDateTime.now())
                .setSupplierSkuMappingCheckLogin("auto-accepted")
            ))
            .endStep()
            .execute();
    }

    @Test
    public void testAutoApproveNewSupplierMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withPartnerMapping(
                OfferBuilder.skuMapping(MARKET_SKU_ID_1))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        DataCampOffer.Offer.Builder builder = dcOffer.toBuilder();
        builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().setCardSource(
            DataCampOfferContent.CardSource.newBuilder().setCardByMskuSearch(true).setMarketSkuId(MARKET_SKU_ID_1).build()
        );

        Offer offer = testDCOffer(blueSupplierUnderBiz(OfferTestUtils.BLUE_SUPPLIER_ID_1));
        offer
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(MODEL_PARENT_ID)
            .setMappedModelId(MODEL_PARENT_ID)
            .setMappedModelConfidence(Offer.MappingConfidence.PARTNER)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setMappedCategoryId(null)
            .setMappedCategoryConfidence(null)
            .setSupplierCategoryId(TEST_CATEGORY_INFO_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
            .setMappedCategoryId(TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
            .setSupplierSkuMapping(MAPPING_1)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierModelMappingId(MODEL_PARENT_ID)
            .setSupplierModelMappingStatus(Offer.MappingStatus.AUTO_ACCEPTED)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);

        Model model = new Model().setId(1L).setSkuParentModelId(1L).setCategoryId(TEST_CATEGORY_INFO_ID);
        offerMappingActionService.APPROVED.setSkuMapping(
            offer,
            new ApprovedSkuMappingContext()
                .setSkuMappingContext(SkuMappingContext.fromSku(model))
                .setMappingConfidence(Offer.MappingConfidence.CONTENT)
        );
        offerRepository.insertOffers(offer);

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, builder.build())))
            .expectedState(DCPipelineState.onlyOffer(offer
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED)
                .setApprovedSkuMappingInternal(MAPPING_1)
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setAutoSkuMapping(MAPPING_1)
                .setSupplierModelMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setAutoApprovedMappingSource(Offer.AutoApprovedMappingSource.AUTO_APPROVE_FROM_SEARCH)
                .setAutoApprovedMapping(true)
                .setDatacampSkuIdFromSearch(MARKET_SKU_ID_1)
                .setDatacampSkuIdFromSearchStatus(Offer.MappingStatus.ACCEPTED)
                .setSupplierSkuMappingCheckTs(LocalDateTime.now())
                .setSupplierSkuMappingCheckLogin("auto-accepted")
            ))
            .endStep()
            .execute();
    }
}
