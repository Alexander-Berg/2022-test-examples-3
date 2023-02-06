package ru.yandex.market.core.supplier.promo.service;

import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.market.mbi.datacamp.saas.impl.DataCampSaasConversions.datacampShopFilterFromRequest;

@DbUnitDataSet(before = "saasWithDatacampPromoServiceTest/datacamp-promo-service.before.csv")
public class SaasWithDatacampPromoServiceTest extends FunctionalTest {
    @Autowired
    @Qualifier("saasDataCampShopService")
    private SaasService saasDataCampShopService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private SaasWithDatacampPromoService saasWithDatacampPromoService;

    @Test
    // первый запрос отрабатывает, второй нет
    // должен вернуться стрим с офферами из успешного запроса
    public void testWhenSaasTupit() {
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
                .when(saasDataCampShopService).searchBusinessOffers(datacampShopFilterFromRequest(firstRequest));

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
        ).when(dataCampShopClient).searchBusinessOffers(any(SearchBusinessOffersRequest.class));

        doThrow(new RuntimeException("Ne mogu"))
                .when(saasDataCampShopService).searchBusinessOffers(datacampShopFilterFromRequest(secondRequest));

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

        doReturn(unitedResponse).when(dataCampShopClient).getBusinessUnitedOffers(anyLong(), anyCollection(), any());

        SaasOfferInfo saasOfferInfo = SaasOfferInfo.newBuilder()
                .addShopId(774L)
                .addOfferId("0516465165")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult)
                .when(saasDataCampShopService).searchBusinessOffers(any());

        List<DataCampOffer.Offer> response =
                saasWithDatacampPromoService.getOffersFromSaasThenStroller(firstRequest);
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
}
