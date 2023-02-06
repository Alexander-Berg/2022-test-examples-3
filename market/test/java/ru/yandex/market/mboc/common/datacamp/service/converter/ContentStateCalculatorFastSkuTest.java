package ru.yandex.market.mboc.common.datacamp.service.converter;

import java.util.List;

import Market.DataCamp.DataCampContentStatus;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentStateCalculatorFastSkuTest {

    // есть маппинг на БК, офер в IN_PROCESS
    @Test
    public void whenMappedToFastAndInProcessThenBasicCpaIsInWork() {
        var offer = createOffer(Offer.ProcessingStatus.IN_PROCESS);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateBasicStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK);
    }

    @Test
    public void whenMappedToFastAndInProcessThenServiceCpaIsCardCreating() {
        var offer = createOffer(Offer.ProcessingStatus.IN_PROCESS);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_CREATING);
    }

    @Test
    public void whenMappedToFastAndInProcessThenBasicCpcIsReady() {
        var offer = createOffer(Offer.ProcessingStatus.IN_PROCESS);

        DataCampContentStatus.OfferContentCpcState result =
            ContentStateCalculator.calculateBasicStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                true);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY);
    }

    @Test
    public void whenMappedToFastAndInProcessThenServiceCpcIsReady() {
        var offer = createOffer(Offer.ProcessingStatus.IN_PROCESS);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY);
    }

    // есть маппинг на БК, офер в NEED_CONTENT (ждем параметры для ПСКЮ)
    @Test
    public void whenMappedToFastAndNeedContentThenBasicCpaIsNeedContent() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateBasicStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT);
    }

    @Test
    public void whenMappedToFastAndNeedContentThenServiceCpaIsNeedContent() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT);
    }

    @Test
    public void whenMappedToFastAndNeedContentThenBasicCpcIsReady() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT);

        DataCampContentStatus.OfferContentCpcState result =
            ContentStateCalculator.calculateBasicStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY);
    }

    @Test
    public void whenMappedToFastAndNeedContentThenServiceCpcIsReady() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY);
    }

    // есть маппинг на БК, офер в CONTENT_PROCESSING (создаем ПСКЮ)
    @Test
    public void whenMappedToFastAndContentProcessingThenBasicCpaIsContentProcessing() {
        var offer = createOffer(Offer.ProcessingStatus.CONTENT_PROCESSING);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateBasicStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING);
    }

    @Test
    public void whenMappedToFastAndContentProcessingThenServiceCpaIsContentProcessing() {
        var offer = createOffer(Offer.ProcessingStatus.CONTENT_PROCESSING);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING);
    }

    @Test
    public void whenMappedToFastAndContentProcessingThenBasicCpcIsContentProcessing() {
        var offer = createOffer(Offer.ProcessingStatus.CONTENT_PROCESSING);

        DataCampContentStatus.OfferContentCpcState result =
            ContentStateCalculator.calculateBasicStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING);
    }

    @Test
    public void whenMappedToFastAndContentProcessingThenServiceCpcIsContentProcessing() {
        var offer = createOffer(Offer.ProcessingStatus.CONTENT_PROCESSING);

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING);
    }

    // есть маппинг на БК, офер в NEED_CONTENT, есть ошибки в offer.getContentStatusActiveError
    @Test
    public void whenMappedToFastAndContentProcessingFailedThenBasicCpaIsCardCreateError() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT)
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU));

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateBasicStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_CREATE_ERROR);
    }

    @Test
    public void whenMappedToFastAndContentProcessingFailedThenServiceCpaIsCardCreateError() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT)
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU));

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpa(offer, Offer.AcceptanceStatus.OK);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_CREATE_ERROR);
    }

    @Test
    public void whenMappedToFastAndContentProcessingFailedThenBasicCpcIsCardUpdateError() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT)
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU));

        DataCampContentStatus.OfferContentCpcState result =
            ContentStateCalculator.calculateBasicStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_CARD_UPDATE_ERROR);
    }

    @Test
    public void whenMappedToFastAndContentProcessingFailedThenServiceCpcIsCardUpdateError() {
        var offer = createOffer(Offer.ProcessingStatus.NEED_CONTENT)
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU));

        DataCampContentStatus.OfferContentCpaState result =
            ContentStateCalculator.calculateServiceStateForCpc(
                Context.builder()
                    .category(OfferTestUtils.defaultCategory())
                    .supplier(OfferTestUtils.businessSupplier())
                    .build(),
                offer,
                Offer.AcceptanceStatus.OK,
                createConverterConfig(),
                false);

        assertThat(result).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_UPDATE_ERROR);
    }

    /////////////
    private Offer createOffer(Offer.ProcessingStatus processingStatus) {
        return OfferTestUtils.simpleOffer()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.blueSupplierUnderBiz1())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true)
            .updateProcessingStatusIfValid(processingStatus)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(OfferTestUtils.TEST_SKU_ID, Offer.SkuType.FAST_SKU),
                Offer.MappingConfidence.PARTNER_FAST);
    }

    private ConverterConfig createConverterConfig() {
        return ConverterConfig.builder()
            .allowPsku2EditingByAuthor(true)
            .useVerdictsForErrors(true)
            .explanationsNamespace(DataCampConverterService.MBOC_CI_ERROR)
            .build();
    }
}
