package ru.yandex.market.mbi.datacamp;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.combine.DataCampCombinedClient;
import ru.yandex.market.mbi.datacamp.combine.DataCampShopCombinedClient;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

public class DataCampShopCombinedClientTest {
    private final DataCampClient strollerClientMock = Mockito.mock(DataCampClient.class);
    private final SaasService saasClientMock = Mockito.mock(SaasService.class);
    private final DataCampCombinedClient dataCampCombinedClient =
            new DataCampShopCombinedClient(strollerClientMock, saasClientMock);

    @Test
    void testSearchBusinessOfferWithSaasAndStrollerMode() {
        List<String> mockedOfferIds = List.of("hid.100126194009", "hid.100256639480");
        String promoId = "#6448";
        long partnerId = 10671634;
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(mockedOfferIds.stream()
                        .map(DataCampShopCombinedClientTest::offerMock)
                        .collect(Collectors.toList())
                )
                .setNextPageNumber(2)
                .setTotalCount(3)
                .build();
        when(saasClientMock.searchBusinessOffers(any()))
                .thenReturn(saasSearchResult);

        SyncGetOffer.GetUnitedOffersResponse strollerResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/DataCampShopCombinedClientTest.getUnitedOffersResponse.json",
                getClass()
        );
        when(strollerClientMock.searchBusinessOffers(argThat(
                request -> request.getOfferIds().containsAll(mockedOfferIds)
        )))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(strollerResponse));

        SearchBusinessOffersResult expectedResult = SearchBusinessOffersResult.builder()
                .setOffers(strollerResponse.getOffersList())
                .setTotalCount(3)
                .setNextPageToken("2")
                .build();
        SearchBusinessOffersRequest request = SearchBusinessOffersRequest.builder()
                .setBusinessId(partnerId)
                .setPartnerId(partnerId)
                .addActivePromoIds(Collections.singletonList(promoId))
                .setPageRequest(SeekSliceRequest.firstN(2))
                .build();
        SearchBusinessOffersResult result = dataCampCombinedClient.searchOffersWithSaasThenStroller(request);
        List<Market.DataCamp.DataCampUnitedOffer.UnitedOffer> offersList = strollerResponse.getOffersList();
        Assertions.assertEquals(promoId, offersList.get(0).getServiceMap().get((int) partnerId).getPromos()
                .getAnaplanPromos().getActivePromos().getPromos(0).getId());
        ReflectionAssert.assertReflectionEquals(expectedResult, result);
    }

    @Nonnull
    private static SaasOfferInfo offerMock(String offerId) {
        return SaasOfferInfo.newBuilder()
                .addOfferId(offerId)
                .build();
    }
}
