package ru.yandex.market.mboc.common.datacamp.service.converter;

import java.util.List;

import Market.DataCamp.DataCampContentStatus;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentStateCalculatorCpcTest {

    @Test
    public void whenGoodContentAndInvalidStatusThenNeedContent() {
        var gcOffer = createOffer(Offer.ProcessingStatus.INVALID)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_GOOD_CONTENT);

        Context context = Context.builder()
            .supplier(OfferTestUtils.businessSupplier())
            .category(OfferTestUtils.defaultCategory())
            .build();

        DataCampContentStatus.OfferContentCpcState result = ContentStateCalculator.calculateBasicStateForCpc(
            context, gcOffer, Offer.AcceptanceStatus.OK, createConverterConfig(), true);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT);
    }

    @Test
    public void whenGoodContentNeedInfoStatusThenNeedContent() {
        var gcOffer = createOffer(Offer.ProcessingStatus.NEED_INFO)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_GOOD_CONTENT);

        Context context = Context.builder()
            .supplier(OfferTestUtils.businessSupplier())
            .category(OfferTestUtils.defaultCategory())
            .build();

        DataCampContentStatus.OfferContentCpcState result = ContentStateCalculator.calculateBasicStateForCpc(
            context, gcOffer, Offer.AcceptanceStatus.OK, createConverterConfig(), false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT);
    }

    @Test
    public void whenNotGoodContentNeedContentStatusThenNeedContent() {
        var gcOffer = createOffer(Offer.ProcessingStatus.NEED_CONTENT);

        Context context = Context.builder()
            .supplier(OfferTestUtils.businessSupplier().setNewContentPipeline(false))
            .category(OfferTestUtils.defaultCategory())
            .build();

        DataCampContentStatus.OfferContentCpcState result = ContentStateCalculator.calculateBasicStateForCpc(
            context, gcOffer, Offer.AcceptanceStatus.OK, createConverterConfig(), false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT);
    }

    private Offer createOffer(Offer.ProcessingStatus processingStatus) {
        return OfferTestUtils.simpleOffer()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.blueSupplierUnderBiz1())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true)
            .updateProcessingStatusIfValid(processingStatus);
    }

    private ConverterConfig createConverterConfig() {
        return ConverterConfig.builder()
            .allowPsku2EditingByAuthor(true)
            .useVerdictsForErrors(true)
            .explanationsNamespace(DataCampConverterService.MBOC_CI_ERROR)
            .build();
    }
}
