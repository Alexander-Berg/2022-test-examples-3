package ru.yandex.market.mboc.common.services.ultracontroller;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UltraControllerServiceImplTest {

    private UltraControllerServiceImpl ultraControllerService;
    private ru.yandex.market.ir.http.UltraControllerService ucRemoteService;

    @Before
    public void setUp() {
        ucRemoteService = mock(ru.yandex.market.ir.http.UltraControllerService.class);
        when(ucRemoteService.enrich(any()))
            .thenReturn(UltraController.DataResponse.newBuilder()
                .addOffers(UltraController.EnrichedOffer.newBuilder())
                .build());
        ultraControllerService = new UltraControllerServiceImpl(
            ucRemoteService,
            UltraControllerServiceImpl.DEFAULT_RETRY_COUNT,
            UltraControllerServiceImpl.DEFAULT_RETRY_SLEEP_MS);
    }

    @Test
    public void whenOfferHasPriceAndUsePriceThenPriceIsSentToUC() {
        Offer offer = OfferTestUtils.nextOffer()
            .storeOfferContent(OfferContent.builder()
                .extraShopFields(ImmutableMap.of(
                    "test", "test value",
                    "Цена", "1000"))
                .build());

        ultraControllerService.enrich(List.of(offer), true);

        ArgumentCaptor<UltraController.DataRequest> ucRequestCaptor =
            ArgumentCaptor.forClass(UltraController.DataRequest.class);
        verify(ucRemoteService, times(1))
            .enrich(ucRequestCaptor.capture());

        UltraController.DataRequest ucRequest = ucRequestCaptor.getValue();

        assertThat(ucRequest).isNotNull();
        assertThat(ucRequest.getOffersCount()).isEqualTo(1);

        UltraController.Offer ucOffer = ucRequest.getOffers(0);

        assertThat(ucOffer.getUsePriceRange()).isTrue();
        assertThat(ucOffer.getYmlParamList())
            .containsExactlyInAnyOrder(
                ru.yandex.market.ir.http.Offer.YmlParam.newBuilder()
                    .setName("test")
                    .setValue("test value")
                    .build(),
                ru.yandex.market.ir.http.Offer.YmlParam.newBuilder()
                    .setName("Цена")
                    .setValue("1000")
                    .build()
            );
    }

    @Test
    public void whenOfferHasNoPriceAndUsePriceThenPriceIsNotSentToUC() {
        Offer offer = OfferTestUtils.nextOffer()
            .storeOfferContent(OfferContent.builder()
                .extraShopFields(ImmutableMap.of(
                    "test", "test value"))
                .build());

        ultraControllerService.enrich(List.of(offer), true);

        ArgumentCaptor<UltraController.DataRequest> ucRequestCaptor =
            ArgumentCaptor.forClass(UltraController.DataRequest.class);
        verify(ucRemoteService, times(1))
            .enrich(ucRequestCaptor.capture());

        UltraController.DataRequest ucRequest = ucRequestCaptor.getValue();

        assertThat(ucRequest).isNotNull();
        assertThat(ucRequest.getOffersCount()).isEqualTo(1);

        UltraController.Offer ucOffer = ucRequest.getOffers(0);

        assertThat(ucOffer.getUsePriceRange()).isFalse();
        assertThat(ucOffer.getYmlParamList())
            .containsExactlyInAnyOrder(
                ru.yandex.market.ir.http.Offer.YmlParam.newBuilder()
                    .setName("test")
                    .setValue("test value")
                    .build()
            );
    }

    @Test
    public void whenOfferHasPriceAndNotUsePriceThenPriceIsNotSentToUC() {
        Offer offer = OfferTestUtils.nextOffer()
            .storeOfferContent(OfferContent.builder()
                .extraShopFields(ImmutableMap.of(
                    "test", "test value",
                    "Цена", "1000"))
                .build());

        ultraControllerService.enrich(List.of(offer), false);

        ArgumentCaptor<UltraController.DataRequest> ucRequestCaptor =
            ArgumentCaptor.forClass(UltraController.DataRequest.class);
        verify(ucRemoteService, times(1))
            .enrich(ucRequestCaptor.capture());

        UltraController.DataRequest ucRequest = ucRequestCaptor.getValue();

        assertThat(ucRequest).isNotNull();
        assertThat(ucRequest.getOffersCount()).isEqualTo(1);

        UltraController.Offer ucOffer = ucRequest.getOffers(0);

        assertThat(ucOffer.getUsePriceRange()).isFalse();
        assertThat(ucOffer.getYmlParamList())
            .containsExactlyInAnyOrder(
                ru.yandex.market.ir.http.Offer.YmlParam.newBuilder()
                    .setName("test")
                    .setValue("test value")
                    .build()
            );
    }
}
