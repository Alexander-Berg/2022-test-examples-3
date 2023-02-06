package ru.yandex.market.mboc.common.datacamp.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

public class DataCampServiceImplTest {

    public static final String DATACAMP_URL = "http://datacamp.white.tst.vs.market.yandex.net/v1";

    private static final long BUSINESS_ID = 123;
    private static final int SHOP_ID = 12345;
    private static final long GROUP_ID = 1;

    private RestTemplate restTemplate;
    private DataCampService dataCampService;

    @Before
    public void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        dataCampService = new DataCampServiceImpl(restTemplate, DATACAMP_URL);
    }

    @Test
    public void testGetOffers() throws Exception {
        Mockito.when(restTemplate.getForEntity(Mockito.any(URI.class), Mockito.any()))
            .thenReturn(ResponseEntity.ok()
                .body(SyncGetOffer.GetUnitedOffersResponse.newBuilder().build()));

        dataCampService.getOffers(BUSINESS_ID, GROUP_ID, true);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(restTemplate).getForEntity(uriCaptor.capture(), Mockito.any());

        assertThat(uriCaptor.getValue()).isEqualTo(new URI(
            DATACAMP_URL + "/partners/123/groups/1?mode=mboc&force=true"));
    }

    @Test
    public void testGetUnitedOffer() throws Exception {
        Mockito.when(restTemplate.getForEntity(Mockito.any(URI.class), Mockito.any()))
            .thenReturn(ResponseEntity.ok()
                .body(SyncGetOffer.GetUnitedOffersResponse.newBuilder()
                    .addOffers(DataCampUnitedOffer.UnitedOffer.newBuilder().build())
                    .build()));

        dataCampService.getUnitedOffer(BUSINESS_ID, "abc&def");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(restTemplate).getForEntity(uriCaptor.capture(), Mockito.any());

        assertThat(uriCaptor.getValue()).isEqualTo(new URI(
            DATACAMP_URL + "/partners/123/offers?offer_id=abc%26def&full=true"));
    }

    @Test
    public void getUnitedOffersByBusinessSkuKeysTrimmed() {
        var dcIdentifiers = DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setOfferId("shop_sku")
            .setBusinessId((int) BUSINESS_ID)
            .build();

        Mockito.when(restTemplate.getForEntity(Mockito.any(URI.class), Mockito.any()))
            .thenReturn(ResponseEntity.ok()
                .body(createGetUnitedOffersResponse(BUSINESS_ID, "shop_sku", SHOP_ID)));

        DataCampUnitedOffer.UnitedOffer unitedOffer = dataCampService
            .getUnitedOffersByBusinessSkuKeys(List.of(new BusinessSkuKey((int) BUSINESS_ID, "shop_sku")))
            .get(0);


        assertThat(unitedOffer.getActualMap())
            .as("all actual map is trimmed")
            .isEmpty();
        assertThat(unitedOffer.getBasic())
            .as("basic offer is not trimmed")
            .usingRecursiveComparison()
            .isEqualTo(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(dcIdentifiers)
                .build());
        assertThat(unitedOffer.getServiceMap().keySet())
            .as("service map keys are not removed")
            .containsExactlyInAnyOrder(SHOP_ID);
        assertThat(unitedOffer.getServiceMap().values())
            .as("service data is trimmed except identifiers and status")
            .containsExactlyInAnyOrder(
                DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(dcIdentifiers.toBuilder().setShopId(SHOP_ID))
                    .setStatus(DataCampOfferStatus.OfferStatus.newBuilder())
                    .build()
            );
    }

    @Test
    public void testGetUnitedOfferTrimmed() {
        var dcIdentifiers = DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setOfferId("shop_sku")
            .setBusinessId((int) BUSINESS_ID)
            .build();

        Mockito.when(restTemplate.getForEntity(Mockito.any(URI.class), Mockito.any()))
            .thenReturn(ResponseEntity.ok()
                .body(createGetUnitedOffersResponse(BUSINESS_ID, "shop_sku", SHOP_ID)));

        DataCampUnitedOffer.UnitedOffer unitedOffer = dataCampService.getUnitedOffer(BUSINESS_ID, "shop_sku");

        assertThat(unitedOffer.getActualMap())
            .as("all actual map is trimmed")
            .isEmpty();
        assertThat(unitedOffer.getBasic())
            .as("basic offer is not trimmed")
            .usingRecursiveComparison()
            .isEqualTo(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(dcIdentifiers)
                .build());
        assertThat(unitedOffer.getServiceMap().keySet())
            .as("service map keys are not removed")
            .containsExactlyInAnyOrder(SHOP_ID);
        assertThat(unitedOffer.getServiceMap().values())
            .as("service data is trimmed except identifiers and status")
            .containsExactlyInAnyOrder(
                DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(dcIdentifiers.toBuilder().setShopId(SHOP_ID))
                    .setStatus(DataCampOfferStatus.OfferStatus.newBuilder())
                    .build()
            );
    }

    private SyncGetOffer.GetUnitedOffersResponse createGetUnitedOffersResponse(long businessId,
                                                                               String shopSku,
                                                                               int... shopIds) {
        var dcIdentifiers = DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setBusinessId((int) businessId)
            .setOfferId(shopSku)
            .build();
        return SyncGetOffer.GetUnitedOffersResponse.newBuilder()
            .addOffers(DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(dcIdentifiers)
                    .build())
                .putAllActual(Arrays.stream(shopIds).boxed()
                    .collect(Collectors.toMap(
                        Function.identity(),
                        shopId -> DataCampUnitedOffer.ActualOffers.newBuilder().build()
                    )))
                .putAllService(Arrays.stream(shopIds).boxed()
                    .collect(Collectors.toMap(
                        Function.identity(),
                        shopId -> DataCampOffer.Offer.newBuilder()
                            .setIdentifiers(dcIdentifiers.toBuilder().setShopId(shopId))
                            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder())
                            .setContent(DataCampOfferContent.OfferContent.newBuilder())
                            .build()
                    )))
            )
            .build();
    }
}
