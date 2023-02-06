package ru.yandex.market.mbi.datacamp;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.combine.DataCampCombinedClient;
import ru.yandex.market.mbi.datacamp.combine.DataCampCombinedClientImpl;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DataCampSearchAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class DataCampCombinedClientImplTest {

    private final DataCampClient strollerClientMock = Mockito.mock(DataCampClient.class);
    private final SaasService saasClientMock = Mockito.mock(SaasService.class);
    private final DataCampCombinedClient dataCampCombinedClient =
            new DataCampCombinedClientImpl(strollerClientMock, saasClientMock);

    @Test
    void testSearchBusinessOfferWithSaasAndStrollerMode() {
        List<String> mockedOfferIds = List.of("offer1", "offer2");
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(mockedOfferIds.stream()
                        .map(DataCampCombinedClientImplTest::offerMock)
                        .collect(Collectors.toList())
                )
                .setPreviousPageNumber(2)
                .setNextPageNumber(4)
                .setTotalCount(43)
                .build();
        when(saasClientMock.searchBusinessOffers(any()))
                .thenReturn(saasSearchResult);

        SyncGetOffer.GetUnitedOffersResponse strollerResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/DataCampClientTest.getUnitedOffersResponse.json",
                getClass()
        );
        when(strollerClientMock.searchBusinessOffers(argThat(
                request -> request.getOfferIds().containsAll(mockedOfferIds)
        )))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(strollerResponse));

        SearchBusinessOffersResult expectedResult = SearchBusinessOffersResult.builder()
                .setOffers(strollerResponse.getOffersList())
                .setTotalCount(43)
                .setPreviousPageToken("2")
                .setNextPageToken("4")
                .build();

        SearchBusinessOffersRequest request = SearchBusinessOffersRequest.builder()
                .setBusinessId(10460476L)
                .setPartnerId(10460132L)
                .setText("подтяжки")
                .setPageRequest(SeekSliceRequest.firstNAfter(2, "3"))
                .build();
        SearchBusinessOffersResult result = dataCampCombinedClient.searchOffersWithSaasThenStroller(request);
        ReflectionAssert.assertReflectionEquals(expectedResult, result);
    }

    @Test
    void testStrollerClientNotInvokedOnEmptySaasResult() {
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of())
                .setTotalCount(0)
                .build();
        when(saasClientMock.searchBusinessOffers(any()))
                .thenReturn(saasSearchResult);

        SearchBusinessOffersResult expectedResult = SearchBusinessOffersResult.builder()
                .setOffers(List.of())
                .setTotalCount(0)
                .build();

        SearchBusinessOffersRequest request = SearchBusinessOffersRequest.builder()
                .setBusinessId(10460476L)
                .setPartnerId(10460132L)
                .setText("подтяжки")
                .setPageRequest(SeekSliceRequest.firstNAfter(2, "3"))
                .build();
        SearchBusinessOffersResult result = dataCampCombinedClient.searchOffersWithSaasThenStroller(request);
        ReflectionAssert.assertReflectionEquals(expectedResult, result);
        Mockito.verifyZeroInteractions(strollerClientMock);
    }

    @Test
    @DisplayName("Поиск списка предложений с информацией об общем количестве результатов вернул корректный ответ")
    void searchOffersWithTotals_correctData_successful() {
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of())
                .setTotalCount(5)
                .build();
        doReturn(saasSearchResult)
                .when(saasClientMock)
                .searchBusinessOffers(
                        argThat(
                                filter -> filter.getFiltersMap()
                                        .get(DataCampSearchAttribute.SEARCH_SHOP_ID)
                                        .contains("12330")
                        )
                );

        SyncGetOffer.GetUnitedOffersResponse strollerResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/DataCampClientTest.getUnitedOffersResponse.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(strollerResponse))
                .when(strollerClientMock)
                .searchBusinessOffers(
                        argThat(
                                request -> Long.valueOf(12330L).equals(request.getPartnerId())
                        )
                );

        SearchBusinessOffersRequest request = SearchBusinessOffersRequest.builder()
                .setBusinessId(10460476L)
                .setPartnerId(12330L)
                .setText("подтяжки")
                .setPageRequest(SeekSliceRequest.firstNAfter(2, "3"))
                .build();
        SearchBusinessOffersResult result = dataCampCombinedClient.searchOffersWithTotals(request);

        SearchBusinessOffersResult expectedResult = SearchBusinessOffersResult.builder()
                .setOffers(strollerResponse.getOffersList())
                .setTotalCount(5)
                .build();
        ReflectionAssert.assertReflectionEquals(expectedResult, result);
    }

    @Nonnull
    private static SaasOfferInfo offerMock(String offerId) {
        return SaasOfferInfo.newBuilder()
                .addOfferId(offerId)
                .build();
    }
}
