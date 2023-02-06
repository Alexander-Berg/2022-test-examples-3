package ru.yandex.market.mbi.datacamp.saas.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampUnitedOffer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DataCampSearchAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType;
import ru.yandex.market.mbi.datacamp.saas.impl.mapper.SaasDatacampMapperImpl;
import ru.yandex.market.mbi.datacamp.saas.impl.model.FacetByGroupAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOffersFacetFilter;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchFacetResult;
import ru.yandex.market.mbi.datacamp.saas.impl.util.SaasConverter;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.saas.search.SaasSearchException;
import ru.yandex.market.saas.search.SaasSearchRequest;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.datacamp.saas.impl.model.FacetByGroupAttribute.I_RESULT_STATUS;
import static ru.yandex.market.mbi.datacamp.saas.impl.model.FacetByGroupAttribute.MARKET_CATEGORY_ID;

class SaasDatacampServiceTest extends AbstractSaasDatacampTest {
    private final SaasSearchService saasSearchService = mock(SaasSearchService.class);
    private final SaasDatacampService saasDatacampService = new SaasDatacampService(saasSearchService,
            new SaasDatacampMapperImpl(), new SaasConverter());

    @Test
    @DisplayName("Ошибка при конвертации SaasOfferFilter -> SaasSearchRequest")
    public void emptyFilterConversion_fail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> saasDatacampService.searchBusinessOffers(SaasOfferFilter.newBuilder().build()),
                "Can't search with empty filters!"
        );
    }

    @ParameterizedTest
    @DisplayName("Пейджирование и конвертация SaasSearchDocument -> SaasOfferInfo")
    @ValueSource(ints = {1, 2})
    void testResponseConversion_ok(int pageNumber) throws SaasSearchException, IOException {
        SaasSearchResponse response = mock(SaasSearchResponse.class);
        when(response.getDocuments())
                .thenReturn(List.of(
                        convertToDoc(readJson("json/saas-offer-info-master-data.json", ObjectNode.class))
                ));
        when(response.getDocuments(any())).thenCallRealMethod();
        when(saasSearchService.search(any())).thenReturn(response);

        DataCampUnitedOffer.UnitedOffer expectedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/SaasDatacampServiceTest.convertedUnitedOffer.json",
                getClass()
        );

        List<DataCampUnitedOffer.UnitedOffer> result = saasDatacampService.searchBusinessOffers(
                        getDefaultOfferFilterBuilder()
                                .setPageRequest(SeekSliceRequest.firstNAfter(1, pageNumber))
                                .build()
                )
                .getPage()
                .entries()
                .stream()
                .map(DataCampSaasConversions::toUnitedOffer)
                .collect(Collectors.toList());
        List<SaasOfferInfo> expected = List.of(getDefaultOfferInfoBuilder().build());

        assertEquals(expected.size(), result.size());
        ReflectionAssert.assertReflectionEquals(expectedOffer, result.get(0));
    }

    private SaasOfferInfo.Builder getDefaultOfferInfoBuilder() {
        return fillDefaultOfferAttributesBuilder(SaasOfferInfo.newBuilder())
                .addOfferId("100500666")
                .setName("Турелка семечек")
                .setMbocConsistency(true);
    }

    @Test
    @DisplayName("При запросе первой страницы нет токена на предыдущую страницу")
    void testPagination_firstFullPage() throws Exception {
        int totalCount = 15;
        int limit = 5;

        SaasSearchResponse responseMock = mockOuterSaasClientResponse(limit, totalCount);
        when(saasSearchService.search(any()))
                .thenReturn(responseMock);

        SearchBusinessOffersRequest searchBusinessOffersRequest = SearchBusinessOffersRequest.builder()
                .setPageRequest(SeekSliceRequest.firstN(limit))
                .setBusinessId(1L)
                .build();
        SearchBusinessOffersResult result =
                saasDatacampService.searchAndConvertBusinessOffers(searchBusinessOffersRequest);
        assertEquals(totalCount, result.getTotalCount());
        assertTrue(result.getResult().prevSliceKey().isEmpty());
        assertEquals("1", result.getResult().nextSliceKey().orElse(null));
    }

    @Test
    @DisplayName("При запросе второй и более страницы есть токен на предыдущую страницу")
    void testPagination_middleFullPage() throws Exception {
        int totalCount = 30;
        int limit = 5;

        SaasSearchResponse responseMock = mockOuterSaasClientResponse(limit, totalCount);
        when(saasSearchService.search(any()))
                .thenReturn(responseMock);

        SearchBusinessOffersRequest searchBusinessOffersRequest = SearchBusinessOffersRequest.builder()
                .setPageRequest(SeekSliceRequest.firstNAfter(limit, String.valueOf(4)))
                .setBusinessId(1L)
                .build();
        SearchBusinessOffersResult result =
                saasDatacampService.searchAndConvertBusinessOffers(searchBusinessOffersRequest);
        assertEquals(totalCount, result.getTotalCount());
        assertEquals("3", result.getResult().prevSliceKey().orElse(null));
        assertEquals("5", result.getResult().nextSliceKey().orElse(null));
    }

    @Test
    @DisplayName("Тест поиска при группировочных аттрибутах")
    void testSearchWithFacets() throws Exception {
        SaasSearchResponse responseMock = mock(SaasSearchResponse.class);
        ArgumentCaptor<SaasSearchRequest> searchRequest = ArgumentCaptor.forClass(SaasSearchRequest.class);
        Map<String, Integer> resultMap = new HashMap<>();

        when(responseMock.getTotal()).thenReturn(204);
        when(responseMock.getFacets(eq(I_RESULT_STATUS))).thenReturn(Collections.emptyMap());
        when(responseMock.getFacets(eq(MARKET_CATEGORY_ID))).thenReturn(resultMap);

        when(saasSearchService.search(searchRequest.capture())).thenReturn(responseMock);
        SaasOffersFacetFilter saasFilter = new SaasOffersFacetFilter(
                10734664L,
                10734664L,
                Set.of(FacetByGroupAttribute.MARKET_CATEGORY_ID, I_RESULT_STATUS)
        );

        saasDatacampService.searchByGroupFacets(saasFilter);
        SaasSearchFacetResult result = saasDatacampService.searchByGroupFacets(saasFilter);
        assertEquals(result.getTotalCount(), 204);

        assertTrue(result.getResultFacetsMap().containsKey(I_RESULT_STATUS));
        assertTrue(result.getResultFacetsMap().get(I_RESULT_STATUS).isEmpty());

        assertTrue(result.getResultFacetsMap().containsKey(FacetByGroupAttribute.MARKET_CATEGORY_ID));
        assertEquals(result.getResultFacetsMap().get(I_RESULT_STATUS), resultMap);

        assertEquals(searchRequest.getValue().getPage(), 0);
        assertEquals(searchRequest.getValue().getCount(), 0);
        assertTrue(searchRequest.getValue().getFacetByGroup().contains(I_RESULT_STATUS));
        assertTrue(searchRequest.getValue().getFacetByGroup().contains(MARKET_CATEGORY_ID));
    }

    @Test
    @DisplayName("Тест поиска при группировочных аттрибутах c фильтром")
    void testSearchWithFacetsWithFilter() throws Exception {
        SaasSearchResponse responseMock = mock(SaasSearchResponse.class);
        ArgumentCaptor<SaasSearchRequest> searchRequest = ArgumentCaptor.forClass(SaasSearchRequest.class);
        Map<String, Integer> resultMap = new HashMap<>();

        when(responseMock.getTotal()).thenReturn(204);
        when(responseMock.getFacets(eq(I_RESULT_STATUS))).thenReturn(Collections.emptyMap());
        when(responseMock.getFacets(eq(MARKET_CATEGORY_ID))).thenReturn(resultMap);

        when(saasSearchService.search(searchRequest.capture())).thenReturn(responseMock);
        SaasOffersFacetFilter saasFilter = new SaasOffersFacetFilter(
                10734664L,
                10734664L,
                Set.of(FacetByGroupAttribute.MARKET_CATEGORY_ID, I_RESULT_STATUS)
        );
        SaasOfferFilter saasOfferFilter = SaasOfferFilter.newBuilder()
                .setPrefix(10734664L)
                .setBusinessId(10734664L)
                .addCreationTs(1000L, 2000L)
                .setPageRequest(SeekSliceRequest.firstN(0))
                .build();


        saasDatacampService.searchByGroupFacetsWithFilters(saasOfferFilter, saasFilter);
        SaasSearchFacetResult result = saasDatacampService.searchByGroupFacetsWithFilters(saasOfferFilter, saasFilter);
        assertEquals(result.getTotalCount(), 204);

        assertTrue(result.getResultFacetsMap().containsKey(I_RESULT_STATUS));
        assertTrue(result.getResultFacetsMap().get(I_RESULT_STATUS).isEmpty());

        assertTrue(result.getResultFacetsMap().containsKey(FacetByGroupAttribute.MARKET_CATEGORY_ID));
        assertEquals(result.getResultFacetsMap().get(I_RESULT_STATUS), resultMap);

        assertEquals(searchRequest.getValue().getPage(), 0);
        assertEquals(searchRequest.getValue().getCount(), 0);
        assertEquals(searchRequest.getValue().getSearchMap()
                .get(DataCampSearchAttribute.CREATION_HOUR_TS), List.of("1000..2000"));
        assertTrue(searchRequest.getValue().getFacetByGroup().contains(I_RESULT_STATUS));
        assertTrue(searchRequest.getValue().getFacetByGroup().contains(MARKET_CATEGORY_ID));
    }

    @Test
    @DisplayName("Тест поиска при группировочных аттрибутах без фильтра по офферам")
    void testSearchWithFacetsWithEmptyFilter() throws Exception {
        SaasSearchResponse responseMock = mock(SaasSearchResponse.class);
        ArgumentCaptor<SaasSearchRequest> searchRequest = ArgumentCaptor.forClass(SaasSearchRequest.class);
        Map<String, Integer> resultMap = new HashMap<>();

        when(responseMock.getTotal()).thenReturn(204);
        when(responseMock.getFacets(eq(I_RESULT_STATUS))).thenReturn(Collections.emptyMap());
        when(responseMock.getFacets(eq(MARKET_CATEGORY_ID))).thenReturn(resultMap);

        when(saasSearchService.search(any())).thenReturn(responseMock);
        SaasOffersFacetFilter saasFilter = new SaasOffersFacetFilter(
                10734664L,
                10734664L,
                Set.of(FacetByGroupAttribute.MARKET_CATEGORY_ID, I_RESULT_STATUS)
        );


        saasDatacampService.searchByGroupFacetsWithFilters(null, saasFilter);
        SaasSearchFacetResult result = saasDatacampService.searchByGroupFacets(saasFilter);
        assertEquals(result.getTotalCount(), 204);
    }

    @Test
    @DisplayName("При запросе второй и более страницы есть токен на предыдущую страницу")
    void testPagination_middleNotFullPage() throws Exception {
        int totalCount = 23;
        int limit = 5;

        SaasSearchResponse responseMock = mockOuterSaasClientResponse(limit - 2, totalCount);
        when(saasSearchService.search(any()))
                .thenReturn(responseMock);

        SearchBusinessOffersRequest searchBusinessOffersRequest = SearchBusinessOffersRequest.builder()
                .setPageRequest(SeekSliceRequest.firstNAfter(limit, String.valueOf(4)))
                .setBusinessId(1L)
                .build();
        SearchBusinessOffersResult result =
                saasDatacampService.searchAndConvertBusinessOffers(searchBusinessOffersRequest);
        assertEquals(totalCount, result.getTotalCount());
        assertEquals("3", result.getResult().prevSliceKey().orElse(null));
        assertTrue(result.getResult().nextSliceKey().isEmpty());
    }

    @Nonnull
    private static SaasSearchResponse mockOuterSaasClientResponse(int limit, int totalCount) {
        SaasSearchResponse responseMock = mock(SaasSearchResponse.class);
        when(responseMock.getDocuments(any())).thenReturn(
                IntStream.range(0, limit)
                        .mapToObj(String::valueOf)
                        .map(SaasDatacampServiceTest::mockOfferInfo)
                        .collect(Collectors.toList())
        );
        when(responseMock.getTotal()).thenReturn(totalCount);
        return responseMock;
    }

    @Nonnull
    private static SaasOfferInfo mockOfferInfo(String offerId) {
        return SaasOfferInfo.newBuilder()
                .addOfferId(offerId)
                .build();
    }

    /*
    Для избежания запусков в аркадии, если падает с ошибкой
    Failed SaaS search request with prefix - это нормально
    в методе ru.yandex.market.saas.search.SaasSearchService.baseSearch
    при дебаге можно получить запрос и запустить руками через curl - будет работать
    */
    @Disabled
    @DisplayName("Тест можно использовать для получения реальных данных с datacamp.")
    @Test
    void testTotalNumberOfDocumentsReal() {
        SaasSearchService searchService = new SaasSearchService("prestable-market-idx.saas.yandex.net",
                80, "market_datacamp_meta");
        SaasService saasService = new SaasDatacampService(searchService, new SaasDatacampMapperImpl(),
                new SaasConverter());
        SaasOfferFilter saasFilter = SaasOfferFilter.newBuilder()
                .setBusinessId(10734664L)
                .setPrefix(10734664L)
                .addShopIds(List.of(10770534L))
                .setDocType(SaasDocType.OFFER)
                .setPageRequest(SeekSliceRequest.firstN(100))
                .build();
        int numberOfOffers = saasService.searchBusinessOffers(saasFilter).getTotalCount();
        assertThat("Number of offers must be greater 0", numberOfOffers, greaterThan(0));
    }

    /*
    Для избежания запусков в аркадии, если падает с ошибкой
    Failed SaaS search request with prefix - это нормально
    в методе ru.yandex.market.saas.search.SaasSearchService.baseSearch
    при дебаге можно получить запрос и запустить руками через curl - будет работать
    */
    @Disabled
    @DisplayName("Тест можно использовать для получения реальных данных с datacamp.")
    @Test
    void testTotalNumberOfDocumentsRealWithGroupingFacets() {
        SaasSearchService searchService = new SaasSearchService("prestable-market-idx.saas.yandex.net",
                80, "market_datacamp_meta");
        SaasService saasService = new SaasDatacampService(searchService, new SaasDatacampMapperImpl(),
                new SaasConverter());
        SaasOffersFacetFilter saasFilter = new SaasOffersFacetFilter(
                10734664L,
                10734664L,
                Set.of(FacetByGroupAttribute.MARKET_CATEGORY_ID, I_RESULT_STATUS)
        );
        int numberOfOffers = saasService.searchByGroupFacets(saasFilter).getTotalCount();
        assertThat("Number of offers must be greater 0", numberOfOffers, greaterThan(0));
    }
}
