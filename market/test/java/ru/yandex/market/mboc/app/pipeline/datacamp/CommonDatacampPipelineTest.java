package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.time.Instant;
import java.util.List;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;

import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static Market.DataCamp.DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY;
import static Market.DataCamp.DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.blueSupplierUnderBiz;

/**
 * @author apluhin
 * @created 3/24/22
 */
public class CommonDatacampPipelineTest extends BaseDatacampPipelineTest {

    @Test
    public void checkAntiMappingToDatacamp() {
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
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setMappedCategoryId(null)
            .setMappedCategoryConfidence(null)
            .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED)
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.PARTNER)
            .setSupplierSkuMapping(MAPPING_1)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierModelMappingId(MODEL_PARENT_ID)
            .setSupplierModelMappingStatus(Offer.MappingStatus.AUTO_ACCEPTED)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);

        offerRepository.insertOffers(offer);
        Offer savedOffer = offerRepository.findAll().get(0);
        antiMappingRepository.insertBatch(
            List.of(
                new AntiMapping().setOfferId(savedOffer.getId()).setNotModelId(1L),
                new AntiMapping().setOfferId(savedOffer.getId()).setNotModelId(2L),
                new AntiMapping().setOfferId(savedOffer.getId()).setNotSkuId(3L),
                new AntiMapping().setOfferId(savedOffer.getId()).setNotModelId(4L).setDeletedTs(Instant.now())
            )
        );

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Send datacamp states")
            .action(tmsSendDataCampOfferStates())
            .addChecks(List.of(verifyContentSystemStatusHandle(builder.build(), (st) -> {
                    st.setAllowModelCreateUpdate(false);
                    st.setCpcState(CPC_CONTENT_READY);
                    st.setCpaState(CONTENT_STATE_READY);
                }),
                (s, dcPipelineState) -> {
                    var identifiers = dcOffer.getIdentifiers();
                    var key = new BusinessSkuKey(identifiers.getBusinessId(), identifiers.getOfferId());
                    var sentDCOffer = getLastSentDCOffer(key);
                    DataCampOfferMapping.AntiMapping antiMappingForUc =
                        sentDCOffer.get().getBasic().getContent().getBinding().getAntiMappingForUc();
                    assertThat(antiMappingForUc.getNotModelIdList()).containsExactly(1L, 2L);
                    assertThat(antiMappingForUc.getNotSkuIdList()).containsExactly(3L);
                    AssertionsForClassTypes.assertThat(antiMappingForUc.hasMeta()).isTrue();
                }))
            .expectedState(DCPipelineState.onlyOffer(offer)
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
                    .withAntiMapping(DataCampOfferMapping.AntiMapping.newBuilder().addNotModelId(1L).addNotSkuId(1L))
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                    .withDisabledFlag(false).build())
            )
            .endStep()
            .execute();
    }

}
