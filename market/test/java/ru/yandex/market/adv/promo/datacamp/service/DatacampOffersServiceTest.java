package ru.yandex.market.adv.promo.datacamp.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class DatacampOffersServiceTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;
    @Autowired
    private DatacampOffersService datacampOffersService;

    @Test
    public void testGetBusinessUnitedOffers() {
        OffersBatch.UnitedOffersBatchResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "DatacampOffersServiceTest/datacamp-get-business-united-offers.json",
                getClass()
        );

        doReturn(getUnitedOffersResponse).when(dataCampClient)
                .getBusinessUnitedOffers(eq(505L), eq(Set.of("VP59136-3SK", "SPG333-111SK")), eq(3L));

        Map<String, List<DataCampOffer.Offer>> offers =
                datacampOffersService.getBusinessOffers(505, Set.of("VP59136-3SK", "SPG333-111SK"), 3L).stream()
                        .collect(Collectors.groupingBy(offer -> offer.getIdentifiers().getOfferId()));

        offers.get("VP59136-3SK").forEach(
                p -> {
                    assertEquals(105569788L, p.getContent().getBinding().getUcMapping().getMarketSkuId());
                    assertEquals("VP59136-3SK", p.getIdentifiers().getOfferId());
                    assertEquals("Санитайзеры", p.getContent().getBinding().getUcMapping().getMarketCategoryName());
                    assertEquals(91042, p.getContent().getBinding().getUcMapping().getMarketCategoryId());
                }
        );

        offers.get("SPG333-111SK").forEach(
                p -> {
                    assertEquals(105569788L, p.getContent().getBinding().getUcMapping().getMarketSkuId());
                    assertEquals("SPG333-111SK", p.getIdentifiers().getOfferId());
                    assertEquals("Санитайзеры", p.getContent().getBinding().getUcMapping().getMarketCategoryName());
                    assertEquals(91042, p.getContent().getBinding().getUcMapping().getMarketCategoryId());
                }
        );
    }

    @Test
    public void testGetBusinessUnitedOffers_OnlyBasic() {
        long businessId = 505L;
        long partnerId = 3L;
        var offerQuery = new OfferQuery.Builder()
                .withIdentifiers(true)
                .build();
        OffersBatch.UnitedOffersBatchResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "DatacampOffersServiceTest/datacamp-get-business-united-offers-only-basic.json",
                getClass()
        );

        doReturn(getUnitedOffersResponse).when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(businessId), eq(Set.of("VP59136-3SK", "SPG333-111SK")), eq(partnerId), eq(offerQuery));

        List<DataCampOffer.Offer> offers =
                datacampOffersService.getBusinessOffers(
                        businessId, Set.of("VP59136-3SK", "SPG333-111SK"), partnerId, offerQuery);

        DataCampOffer.Offer offer1 = offers.get(0);
        DataCampOffer.Offer offer2 = offers.get(1);

        assertEquals(offer1.getIdentifiers().getOfferId(), "VP59136-3SK");
        assertEquals(offer1.getIdentifiers().getShopId(), 150);
        assertEquals(offer2.getIdentifiers().getOfferId(), "SPG333-111SK");
        assertEquals(offer2.getIdentifiers().getShopId(), 150);
    }

    @Test
    public void testGetBusinessUnitedOffers_OnlyService() {
        long businessId = 505L;
        long partnerId = 3L;
        var offerQuery = new OfferQuery.Builder()
                .withPrice(true)
                .build();
        OffersBatch.UnitedOffersBatchResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "DatacampOffersServiceTest/datacamp-get-business-united-offers-only-service.json",
                getClass()
        );

        doReturn(getUnitedOffersResponse).when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(businessId), eq(Set.of("VP59136-3SK", "SPG333-111SK")), eq(partnerId), eq(offerQuery));

        List<DataCampOffer.Offer> offers =
                datacampOffersService.getBusinessOffers(
                        businessId, Set.of("VP59136-3SK", "SPG333-111SK"), partnerId, offerQuery);

        DataCampOffer.Offer offer1 = offers.get(0);
        DataCampOffer.Offer offer2 = offers.get(1);

        assertEquals(offer1.getPrice().getBasic().getBinaryPrice().getPrice(), 1005000000);
        assertEquals(offer2.getPrice().getBasic().getBinaryPrice().getPrice(), 2000000000);
    }

    @Test
    public void testGetBusinessUnitedOffers_OnlyActual() {
        long businessId = 505L;
        long partnerId = 3L;
        var offerQuery = new OfferQuery.Builder()
                .withStockCount(true)
                .withIsDsbs(true)
                .build();
        OffersBatch.UnitedOffersBatchResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "DatacampOffersServiceTest/datacamp-get-business-united-offers-only-actual.json",
                getClass()
        );

        doReturn(getUnitedOffersResponse).when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(businessId), eq(Set.of("VP59136-3SK", "SPG333-111SK")), eq(partnerId), eq(offerQuery));

        List<DataCampOffer.Offer> offers =
                datacampOffersService.getBusinessOffers(
                        businessId, Set.of("VP59136-3SK", "SPG333-111SK"), partnerId, offerQuery);

        DataCampOffer.Offer offer1 = offers.get(0);
        DataCampOffer.Offer offer2 = offers.get(1);

        assertEquals(offer1.getPartnerInfo().getIsDsbs(), false);
        assertEquals(offer1.getStockInfo().getMarketStocks().getCount(), 150);
        assertEquals(offer2.getPartnerInfo().getIsDsbs(), true);
        assertEquals(offer2.getStockInfo().getMarketStocks().getCount(), 200);
    }
}
