package ru.yandex.market.adv.promo.datacamp.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.datacamp.model.BriefSaasPromoInfo;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.readJson;
import static ru.yandex.market.mbi.datacamp.saas.impl.DataCampSaasConversions.datacampShopFilterFromRequest;

public class PromoSaasServiceTest extends FunctionalTest {
    @Autowired
    private PromoSaasService promoSaasService;

    @Autowired
    private SaasService saasService;

    @Autowired
    private DataCampClient dataCampClient;

    @Test
    // первый запрос отрабатывает, второй нет
    // должен вернуться стрим с офферами из успешного запроса
    public void testWhenSaasTupit () {
        SeekSliceRequest.Forward<String> paging1 = SeekSliceRequest.firstN(1);
        SearchBusinessOffersRequest request = createSaasRequest();
        SearchBusinessOffersRequest firstRequest = request.toBuilder()
                .setPageRequest(paging1)
                .setEarlyUrls(true)
                .build();
        SeekSliceRequest.Forward<String> paging2 = SeekSliceRequest.firstNAfter(1, "12");
        SearchBusinessOffersRequest secondRequest = request.toBuilder()
                .setPageRequest(paging2)
                .setEarlyUrls(true)
                .build();

        doReturn(SaasSearchResult.builder()
                .setNextPageNumber(12)
                .setPreviousPageNumber(0)
                .setTotalCount(563)
                .setOffers(List.of(SaasOfferInfo.newBuilder()
                        .addOfferId("offer_id")
                        .build()))
                .build())
                .when(saasService).searchBusinessOffers(datacampShopFilterFromRequest(firstRequest));

        final DataCampOffer.Offer dataCampOffer = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId(1)
                        .setWarehouseId(1)
                        .setOfferId("1")
                        .build())
                .build();

        doReturn(SearchBusinessOffersResult.builder()
                .setOffers(List.of(
                                DataCampUnitedOffer.UnitedOffer.newBuilder()
                                        .setBasic(dataCampOffer)
                                        .build()
                        )
                ).build()
        ).when(dataCampClient).searchBusinessOffers(any(SearchBusinessOffersRequest.class));

        doThrow(new RuntimeException("Ne mogu"))
                .when(saasService).searchBusinessOffers(datacampShopFilterFromRequest(secondRequest));

        OffersBatch.UnitedOffersBatchResponse unitedResponse = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(dataCampOffer)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, dataCampOffer)
                                        .build())
                                .putService(1, dataCampOffer)
                                .build())
                        .build())
                .build();

        doReturn(unitedResponse).when(dataCampClient).getBusinessUnitedOffers(anyLong(), anyCollection(), any());

        SaasOfferInfo saasOfferInfo = SaasOfferInfo.newBuilder()
                .addShopId(774L)
                .addOfferId("0516465165")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo))
                .setTotalCount(1)
                .build();

        doReturn(saasSearchResult)
                .when(saasService).searchBusinessOffers(any());

        Set<String> response =
                promoSaasService.getAllOfferIdsFromSaas(firstRequest);
        Assertions.assertEquals(1, response.size());
    }

    private SearchBusinessOffersRequest createSaasRequest() {
        SearchBusinessOffersRequest.Builder request =
                SearchBusinessOffersRequest.builder()
                        .setBusinessId(1L)
                        .setPartnerId(1L)
                        .setEarlyUrls(true)
                        .setPageRequest(SeekSliceRequest.firstN(1));
        return request.build();
    }

    @Test
    void getPromosForPartnerTest() throws IOException {
        SaasSearchResponse firstPageResponse =
                readJson(this.getClass(), "saas-grouping-response-data.json", SaasSearchResponse.class);
        SaasSearchResponse emptyResponse =
                readJson(this.getClass(), "saas-grouping-empty-response-data.json", SaasSearchResponse.class);
        when(saasService.searchShopGroupings(anyLong(), eq(0), any(), any(), anyInt(), anyInt(), isNull()))
                .thenReturn(firstPageResponse);
        when(saasService.searchShopGroupings(anyLong(), eq(1), any(), any(), anyInt(), anyInt(), isNull()))
                .thenReturn(emptyResponse);

        Map<String, BriefSaasPromoInfo> partnerPromos = promoSaasService.getPromosForPartner(10264538L, false, null);
        Assertions.assertEquals(38, partnerPromos.size());

        Set<String> activePromos = partnerPromos.values().stream()
                .filter(BriefSaasPromoInfo::isParticipating)
                .map(BriefSaasPromoInfo::getPromoId)
                .collect(Collectors.toSet());
        MatcherAssert.assertThat(
                activePromos,
                containsInAnyOrder(
                        "#6633", "#7170", "10264538_YFYAAEGP", "#8667", "10264538_BHRWPFUA", "#6938",
                        "10264538_YNYGBBME", "10264538_RWHGGRKY", "10264538_GUEGZMVC", "10264538_RDFXHGYM",
                        "10264538_KRUYMCDS"
                )
        );

        String activeAnaplanPromoId = "#8667";
        BriefSaasPromoInfo activeAnaplanPromo = new BriefSaasPromoInfo(activeAnaplanPromoId, true, 2);
        Assertions.assertEquals(
                activeAnaplanPromo,
                partnerPromos.values().stream()
                        .filter(promo -> activeAnaplanPromoId.equals(promo.getPromoId()))
                        .findFirst()
                        .orElse(null)
        );

        String activePartnerPromoId = "10264538_BHRWPFUA";
        BriefSaasPromoInfo activePartnerPromo = new BriefSaasPromoInfo(activePartnerPromoId, true, 2);
        Assertions.assertEquals(
                activePartnerPromo,
                partnerPromos.values().stream()
                        .filter(promo -> activePartnerPromoId.equals(promo.getPromoId()))
                        .findFirst()
                        .orElse(null)
        );

        String potentialAnaplanPromoId = "#9056";
        BriefSaasPromoInfo potentialAnaplanPromo = new BriefSaasPromoInfo(potentialAnaplanPromoId, false, 20);
        Assertions.assertEquals(
                potentialAnaplanPromo,
                partnerPromos.values().stream()
                        .filter(promo -> potentialAnaplanPromoId.equals(promo.getPromoId()))
                        .findFirst()
                        .orElse(null)
        );
    }
}
