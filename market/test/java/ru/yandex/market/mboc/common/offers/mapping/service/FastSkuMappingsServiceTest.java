package ru.yandex.market.mboc.common.offers.mapping.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FastSkuMappingsServiceTest {

    private NeedContentStatusService needContentStatusService;

    private FastSkuMappingsService fastSkuMappingsService;

    @Before
    public void setUp() {
        needContentStatusService = Mockito.mock(NeedContentStatusService.class);
        fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
    }

    @Test
    public void whenNeedContentStatusServiceProhibitsFastCreationThenDoNotEraseNotFastMapping() {
        when(needContentStatusService.isFastSkuCreationAllowed(any()))
            .thenReturn(false);

        var offerMsku = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(1L, Offer.SkuType.MARKET),
                Offer.MappingConfidence.CONTENT
            );
        var offerPsku = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(1L, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF
            );

        fastSkuMappingsService.checkAndRemoveFastMappings(offerMsku, offerPsku);

        MbocAssertions.assertThat(offerMsku).hasApprovedMapping(1L);
        MbocAssertions.assertThat(offerPsku).hasApprovedMapping(1L);
    }

    @Test
    public void whenContentProcessingStatusIsNoneAndOfferNotBlueThenEraseFastMapping() {
        when(needContentStatusService.isFastSkuCreationAllowed(any()))
            .thenReturn(true);

        var offer = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(1L, Offer.SkuType.FAST_SKU),
                Offer.MappingConfidence.PARTNER_FAST
            )
            .setOfferDestination(Offer.MappingDestination.DSBS)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE);

        fastSkuMappingsService.checkAndRemoveFastMappings(offer);

        MbocAssertions.assertThat(offer).doesNotHaveApprovedMapping();
    }

    @Test
    public void whenContentProcessingStatusIsNoneThenDoNotEraseNotFastMapping() {
        when(needContentStatusService.isFastSkuCreationAllowed(any()))
            .thenReturn(true);

        var offerMsku = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(1L, Offer.SkuType.MARKET),
                Offer.MappingConfidence.CONTENT
            )
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE);
        var offerPsku = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(1L, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF
            )
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE);

        fastSkuMappingsService.checkAndRemoveFastMappings(offerMsku, offerPsku);

        MbocAssertions.assertThat(offerMsku).hasApprovedMapping(1L);
        MbocAssertions.assertThat(offerPsku).hasApprovedMapping(1L);
    }
}
