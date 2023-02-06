package ru.yandex.market.fmcg.bff.controller;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.market.fmcg.bff.controller.dto.CoordsDto;
import ru.yandex.market.fmcg.bff.controller.dto.FilterParamDto;
import ru.yandex.market.fmcg.bff.controller.dto.MarketProductDto;
import ru.yandex.market.fmcg.bff.controller.dto.ResultsResponseV2;
import ru.yandex.market.fmcg.bff.controller.dto.SearchResultDto;
import ru.yandex.market.fmcg.bff.controller.dto.SearchResultDto.CategoryDto;
import ru.yandex.market.fmcg.bff.controller.dto.SearchResultDto.ItemAvailabilityDto;
import ru.yandex.market.fmcg.bff.controller.dto.SearchResultDto.ItemDto;
import ru.yandex.market.fmcg.bff.controller.dto.SearchResultDto.PagerDto;
import ru.yandex.market.fmcg.bff.controller.dto.ShopOutletDto;
import ru.yandex.market.fmcg.bff.controller.dto.ShopOutletWithCoordsDto;
import ru.yandex.market.fmcg.bff.controller.dto.request.ShopOutletsRequestDto;
import ru.yandex.market.fmcg.bff.suggestion.SuggestionService;
import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.fmcg.bff.test.MockServerUtil;
import ru.yandex.market.fmcg.bff.test.TestUtil;
import ru.yandex.market.fmcg.bff.util.Const;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static ru.yandex.market.fmcg.bff.controller.dto.FilterParamDto.ExpandableType.VENDORS;
import static ru.yandex.market.fmcg.bff.test.TestUtil.loadResourceAsString;

class SearchWithFiltersControllerTest extends FmcgBffTest {
    private static List<Long> mskuIds;

    private ShopOutletsRequestDto shopOutletsRequestNoShopOutletId;

    @Autowired
    SearchWithFiltersController searchWithFiltersController;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    SuggestionService suggestionService;

    private SearchResultDto resultDto;
    private ShopOutletsRequestDto shopOutletsRequest;

    @BeforeEach
    void setUp() {
        mskuIds = new LinkedList<>();
        mskuIds.add(212823278L);
        shopOutletsRequest = initShopOutletRequest();
        resultDto = getResultDto();
        shopOutletsRequestNoShopOutletId = ShopOutletsRequestDto.builder()
            .userCoords(new CoordsDto(55.758878d, 37.563517d))
            .shopOutlets(Arrays.asList(
                new ShopOutletWithCoordsDto(
                    new ShopOutletDto(1L, 2L),
                    new CoordsDto(55.758890d, 37.563616d)
                ),
                new ShopOutletWithCoordsDto(
                    new ShopOutletDto(2L, null),
                    new CoordsDto(55.758848d, 37.563541d)
                ),
                new ShopOutletWithCoordsDto(
                    new ShopOutletDto(3L, 4L),
                    new CoordsDto(0d, 0d)
                )
            ))
            .build();
    }

    @BeforeEach
    void initServer() {
        MockServerUtil.INSTANCE.reset();
    }

    private SearchResultDto getResultDto() {
        resultDto = new SearchResultDto();
        resultDto.setDocumentCount(1);
        resultDto.setPager(new PagerDto(0, 20, 1));
        List<ItemAvailabilityDto> itemAvailabilityDto = Collections.singletonList(new ItemAvailabilityDto(
            "574430", "880", false,
            null, null, null, new ArrayList<>()));
        CategoryDto categoryDto = new CategoryDto("Снэки", 15714670L, 16570646L, false);
        CategoryDto leafCategory = new CategoryDto("Чипсы", 15714671L, 16570648L, false);
        resultDto.setItems(Collections.singletonList(
            new ItemDto("100464699111", 4390L, 4390L, null, itemAvailabilityDto,
                categoryDto, leafCategory, null, null, null)));
        resultDto.setCategories(Arrays.asList(leafCategory));
        resultDto.setFoundAlcohol("Some");
        return resultDto;
    }

    void setSearchTestData(String place, String response) {
        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", place))
            .respond(
                HttpResponse.response(response)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json; charset=utf-8")
            );
    }

    void setupSearchMockServerResponse(String place, String loadResponse) {
        setSearchTestData(place, loadResponse);
    }

    private ShopOutletsRequestDto initShopOutletRequest() {
        return ShopOutletsRequestDto.builder()
            .userCoords(new CoordsDto(0d, 0d))
            .shopOutlets(Collections.singletonList(
                new ShopOutletWithCoordsDto(
                    new ShopOutletDto(574430L, 880L),
                    new CoordsDto(0d, 0d)
                ))).build();
    }

    @Test
    void testVendorsFilterWithSearchPromo() throws Exception {
        final String nid = "16321230";
        final List<String> vendors = Arrays.asList("15798858", "15887047", "16819915", "14434686");
        final String reportResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.testVendorsFilterWithSearchPromo.reportResponse.json");
        final String expectedBffResponseFilterParams = TestUtil.loadResourceAsString(
            "SearchControllerTest.testVendorsFilterWithSearchPromo.bffResponse.filterParams.json");
        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", "yellow_promo")
                .withQueryStringParameter("nid", nid)
                .withQueryStringParameter("fesh", "574430")
                .withQueryStringParameter("outlets", "88057809")
                .withQueryStringParameter("filterList", "vendor")
                .withQueryStringParameter("glfilter", "7893318:" + String.join(",", vendors))
            )
            .respond(
                HttpResponse.response(reportResponse)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
            );
        mockMvc.perform(MockMvcRequestBuilders
            .post("/apiv1/product/searchPromo")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.loadResourceAsString(
                "SearchControllerTest.testVendorsFilterWithSearchPromo.bffRequestBody.json"))
            .param("nid", nid)
            .param("vendors", vendors.toArray(new String[0])))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().json(expectedBffResponseFilterParams));
    }

    @Test
    void testReportExpFlags() throws Exception {
        final String nid = "16321230";
        final String reportResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.ReportResponse.json");

        // non-empty report response for exp turned on
        // cases of single flag and muiltiple flags
        for (String rearrFactors : new String[] {"use_new_yellow_index=1", "blabla=1;use_new_yellow_index=1"}) {
            MockServerUtil.INSTANCE.mockServer()
                .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                    .withQueryStringParameter("rearr-factors", rearrFactors)
                    .withQueryStringParameter("place", "yellow_msku")
                    .withQueryStringParameter("nid", nid)
                    .withQueryStringParameter("fesh", "574430")
                    .withQueryStringParameter("outlets", "88057809")
                )
                .respond(
                    HttpResponse.response(reportResponse)
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                );
        }
        // mark report response with empty response for all requests without rearr
        final String reportEmptyResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.ReportEmptyResponse.json");
        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", "yellow_msku")
                .withQueryStringParameter("nid", nid)
                .withQueryStringParameter("fesh", "574430")
                .withQueryStringParameter("outlets", "88057809")
            )
            .respond(
                HttpResponse.response(reportEmptyResponse)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
            );

        // valid flags: expecting status OK
        final String expFlagsJson = "[{\"HANDLER\": \"MARKETAPPS\", \"CONTEXT\": {\"SUPERCHECK\": {\"TESTID\": [\"102711\"], \"rearr\": [\"use_new_yellow_index=1\"]}}}]";
        final String expFlagsBase64 = Base64.getEncoder().encodeToString(expFlagsJson.getBytes());
        final String bffResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.testReportFlags.bffResponse.json");
        mockMvc.perform(MockMvcRequestBuilders
            .post("/apiv1/product/searchByNid")
            .header("x-yandex-expflags", expFlagsBase64)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.loadResourceAsString(
                "SearchControllerTest.bffRequestBody.json"))
            .param("nid", nid)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().json(bffResponse));

        // invalid flags: expecting 200 status, and empty response
        final String expInvalidFlagsJson = "[{\"HANDLER\": \"MARKETAPPS\"}]";
        final String expInvalidFlagsBase64 = Base64.getEncoder().encodeToString(expInvalidFlagsJson.getBytes());
        String bffEmptyResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.bffEmptyResponse.json");
        final String expBuggyFlagsJson = "[{\"HANDLER\": \"MARKETAPPS\", \"CONTEXT\": {\"SUPERCHECK\": {\"TESTID\": [\"102711\"]}}}]";
        final String expBuggyFlagsBase64 = Base64.getEncoder().encodeToString(expBuggyFlagsJson.getBytes());
        final String expBuggyTestIdsJson = "[{\"HANDLER\": \"MARKETAPPS\", \"CONTEXT\": {\"SUPERCHECK\": {\"rearr\": []}}}]";
        final String expBuggyTestIdsBase64 = Base64.getEncoder().encodeToString(expBuggyTestIdsJson.getBytes());
        for (String flagsBase64 : new String[] {expInvalidFlagsBase64, expBuggyFlagsBase64, expBuggyTestIdsBase64}) {
            mockMvc.perform(MockMvcRequestBuilders
                .post("/apiv1/product/searchByNid")
                .header("x-yandex-expflags", flagsBase64)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(TestUtil.loadResourceAsString(
                    "SearchControllerTest.bffRequestBody.json"))
                .param("nid", nid)
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.content().json(bffEmptyResponse));
        }
        // valid flags in multiple headers: expecting status OK
        // the value of interest is not the first in the header
        final String expFlags2Json = "[{\"HANDLER\": \"MARKETAPPS\", \"CONTEXT\": {\"SUPERCHECK\": {\"TESTID\": [\"123\"], \"rearr\": [\"blabla=1\"]}}}]";
        final String expFlags2Base64 = Base64.getEncoder().encodeToString(expFlags2Json.getBytes());
        mockMvc.perform(MockMvcRequestBuilders
            .post("/apiv1/product/searchByNid")
            .header("x-yandex-expflags", expFlags2Base64, expFlagsBase64)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.loadResourceAsString(
                "SearchControllerTest.bffRequestBody.json"))
            .param("nid", nid)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().json(bffResponse));
        mockMvc.perform(MockMvcRequestBuilders
            .post("/apiv1/product/searchByNid")
            .header("x-yandex-expflags", String.join(",", expFlags2Base64, expFlagsBase64))
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.loadResourceAsString(
                "SearchControllerTest.bffRequestBody.json"))
            .param("nid", nid)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().json(bffResponse));

    }

    @Test
    void testVendorsFilter() {
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportWithVendorParam.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ShopOutletsRequestDto request = ShopOutletsRequestDto.builder()
            .userCoords(new CoordsDto(55.758890, 37.563616))
            .shopOutlets(Collections.singletonList(
                new ShopOutletWithCoordsDto(
                    new ShopOutletDto(574430L, 88057809L),
                    new CoordsDto(55.758890, 37.563616)
                ))
            ).build();
        ResultsResponseV2 resultByNid = searchWithFiltersController.searchByNid(request,
            16321230L, null, null, null, null, 1, 1, null, 1, Arrays.asList(
                15798858L, 15887047L, 16819915L), null
        );
        List<Integer> availableVendors = Arrays.asList(14459498, 14460430, 14460575);
        Map<Integer, String> allVendorMap = new HashMap<Integer, String>() {
            {
                put(14459498, "Домик в деревне");
                put(14460430, "Рузское Молоко");
                put(14460575, "ЭтоЛето");
            }
        };
        assertNotNull(resultByNid.getCollections());
        assertFalse(resultByNid.getCollections().isEmpty());

        List<FilterParamDto> filterParamList =
            (List<FilterParamDto>) resultByNid.getCollections().get(FilterParamDto.ENTITY);

        List<FilterParamDto> notVendorList = filterParamList.stream()
            .filter(filterParamDto -> !filterParamDto.getExpandableType().equals(VENDORS))
            .collect(Collectors.toList());
        for (FilterParamDto notVendorFilter : notVendorList) {
            assertNull(notVendorFilter.getAvailableVendors());
            assertNull(notVendorFilter.getAllVendorsMap());
            assertNull(notVendorFilter.getSelectedVendors());
        }

        List<FilterParamDto> vendorList = filterParamList.stream()
            .filter(filterParamDto -> filterParamDto.getExpandableType().equals(VENDORS))
            .collect(Collectors.toList());
        assertEquals(1, vendorList.size());

        FilterParamDto vendorFilter = vendorList.get(0);

        Assertions.assertThat(
            vendorFilter.getAvailableVendors()
        ).containsOnlyElementsOf(availableVendors);

        Assertions.assertThat(
            vendorFilter.getAllVendorsMap().entrySet()
        ).containsOnlyElementsOf(allVendorMap.entrySet());
    }

    @Test
    void testTypeNoNewEnumValue() {
        //нельзя добавлять поля в enum Type
        //при возникновении новых значений падает Android 2.0.5
        List<String> enumValues = Arrays.asList(FilterParamDto.Type.values()).stream()
            .map(Enum::toString)
            .collect(Collectors.toList());
        Assertions.assertThat(enumValues).containsExactlyInAnyOrder("NUMERIC_RANGE", "BOOLEAN", "STRING");

        //Android 2.0.5 ожидает, что поле type не пустое в ответе
        //его заполнение корректным образом добавлено для обратной совместимости
        FilterParamDto filterParamDto = new FilterParamDto();
        Assert.assertEquals(FilterParamDto.Type.BOOLEAN, filterParamDto.getType());
        Assert.assertNotNull(filterParamDto.getBooleanValue());
    }

    @Test
    void testSearchByBarcodeFromReport() {
        String responseString = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswer.json");
        setupSearchMockServerResponse("yellow_msku", responseString);
        ResultsResponseV2 resultsResponseV2 = searchWithFiltersController.searchByBarcode
            (shopOutletsRequest, "4602112203612", null, null, null, null, null, 0);
        assertFalse(resultsResponseV2.getCollections().isEmpty());
        assertNotNull(resultsResponseV2.getCollections());
    }

    @Test
    void testSearchWithAdultParamWithAlcoholSome() {
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswer.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByText = searchWithFiltersController.searchByText(
            shopOutletsRequest, "4602112203612", null, null, null, null, null,
            0, 20, null, 0, null, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultsByMsku = searchWithFiltersController.searchByMsku(shopOutletsRequest,
            mskuIds, null, null, null, null, 0, 20, true, 1);

        Assertions.assertThat(resultByText.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        assertFalse(resultByText.getCollections().isEmpty());
        assertEquals(resultDto, resultByText.getCollections().get(SearchResultDto.ENTITY).iterator().next());
        assertEquals(resultDto, resultsByMsku.getCollections().get(SearchResultDto.ENTITY).iterator().next());
        resultDto.setFoundAlcohol("All");
        assertNotEquals(resultDto, resultsByMsku.getCollections().get(SearchResultDto.ENTITY).iterator().next());
    }

    @Test
    void testSearchWithHasAlcoOfferParam() {
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswer.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByText = searchWithFiltersController.searchByText(
            shopOutletsRequest, "4602112203612", null, null, null, null, null,
            0, 20, null, 0, null, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultsByMsku = searchWithFiltersController.searchByMsku(shopOutletsRequest,
            mskuIds, null, null, null, null, 0, 20, true, 1);
        initServer();
        setupSearchMockServerResponse("yellow_promo", loadResponse);
        ResultsResponseV2 resultByPromo = searchWithFiltersController.searchPromos(shopOutletsRequest,
            234234L, null, null, null, null, 0, 20, true, 1, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByNid = searchWithFiltersController.searchByNid(shopOutletsRequest,
            234234L, null, null, null, null, 0, 20, true, 1, null, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByBarcode = searchWithFiltersController.searchByBarcode(shopOutletsRequest,
            "4235234523", null, null, null, null, true, 1);
        Assertions.assertThat(resultByText.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        Assertions.assertThat(resultByBarcode.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        Assertions.assertThat(resultByNid.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        Assertions.assertThat(resultByPromo.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        Assertions.assertThat(resultsByMsku.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
    }

    @Test
    void testSearchWithHasAlcoOfferFalse() {
        initServer();
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerHasAlcoOfferFalse.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByText = searchWithFiltersController.searchByText(
            shopOutletsRequest, "4602112203612", null, null, null, null, null,
            0, 20, null, 0, null, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultsByMsku = searchWithFiltersController.searchByMsku(shopOutletsRequest,
            mskuIds, null, null, null, null, 0, 20, true, 1);
        initServer();
        setupSearchMockServerResponse("yellow_promo", loadResponse);
        ResultsResponseV2 resultByPromo = searchWithFiltersController.searchPromos(shopOutletsRequest,
            234234L, null, null, null, null, 0, 20, true, 1, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByNid = searchWithFiltersController.searchByNid(shopOutletsRequest,
            234234L, null, null, null, null, 0, 20, true, 1, null, null);
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultByBarcode = searchWithFiltersController.searchByBarcode(shopOutletsRequest,
            "4235234523", null, null, null, null, true, 1);
        Assertions.assertThat(resultByBarcode.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(false));
        Assertions.assertThat(resultByNid.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(false));
        Assertions.assertThat(resultByPromo.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(false));
        Assertions.assertThat(resultsByMsku.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(false));
    }

    @Test
    void testSearchWithAdultParamWithAlcoholAll() {
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerWithAllAlcohol.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse);
        ResultsResponseV2 resultsByMsku = searchWithFiltersController.searchByMsku(shopOutletsRequest,
            mskuIds, null, null, null, null, 0, 20, null, 1);
        resultDto.setFoundAlcohol("All");
        resultDto.setPager(new PagerDto(0, 20, 0));
        resultDto.setItems(new ArrayList<>());
        resultDto.setDocumentCount(0);
        resultDto.setCategories(new ArrayList<>());
        assertFalse(resultsByMsku.getCollections().isEmpty());
        assertEquals(resultDto, resultsByMsku.getCollections().get(SearchResultDto.ENTITY).iterator().next());
        assertEquals(resultDto, resultsByMsku.getCollections().get(SearchResultDto.ENTITY).iterator().next());
    }

    @Test
    void testSearchWithAdultParamWithAlcoholNone() {
        String loadResponse2 = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerWithAlcoholNone.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse2);
        ResultsResponseV2 resultsResponseV2 = searchWithFiltersController.searchByText(
            shopOutletsRequest, "4602112203612", null, null, null, null, null,
            0, 20, null, 0, null, null);
        resultDto.setFoundAlcohol("None");
        assertFalse(resultsResponseV2.getCollections().isEmpty());
        assertEquals(resultDto, resultsResponseV2.getCollections().get(SearchResultDto.ENTITY).iterator().next());
        initServer();
        setupSearchMockServerResponse("yellow_msku", loadResponse2);
        ResultsResponseV2 resultsByMsku = searchWithFiltersController.searchByMsku(shopOutletsRequest,
            mskuIds, null, null, null, null, 0, 20, true, 1);
        assertEquals(resultDto, resultsByMsku.getCollections().get(SearchResultDto.ENTITY).iterator().next());
        resultDto.setFoundAlcohol("None");
        resultDto.setPager(new PagerDto(0, 20, 0));
        resultDto.setDocumentCount(0);
        resultDto.setItems(new ArrayList<>());
        resultDto.setCategories(new ArrayList<>());
        initServer();
        String loadResponse3 = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerWithAlcoNoneAndEmptyDoc.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse3);
        ResultsResponseV2 resultByText = searchWithFiltersController.searchByText(
            shopOutletsRequest, "4602112203612", null, null, null, null, null,
            0, 20, null, 0, null, null);
        assertEquals(resultDto, resultByText.getCollections().get(SearchResultDto.ENTITY).iterator().next());
    }

    @Test
    void testSearchWithAdultNullReportAlcoholParam() {
        String loadResponse2 = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerAlcoholNull.json");
        setupSearchMockServerResponse("yellow_msku", loadResponse2);
        ResultsResponseV2 resultsResponseV2 = searchWithFiltersController.searchByText(
            shopOutletsRequest, "4602112203612", null, null, null, null, null,
            0, 20, null, 0, null, null);
        // краевой случай когда репорт не вернул никакое значение, возвращаем на фронт "all"
        resultDto.setFoundAlcohol("All");
        assertFalse(resultsResponseV2.getCollections().isEmpty());
        assertEquals(resultDto, resultsResponseV2.getCollections().get(SearchResultDto.ENTITY).iterator().next());
    }


    @Test
    void testAllShopOutLetsFromControllerNoId() {
        IllegalArgumentException exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchWithFiltersController.searchPromos(shopOutletsRequestNoShopOutletId, 16321962L, null, null, null,
                null, 1, 20, null, 0, null);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchWithFiltersController.searchByBarcode(shopOutletsRequestNoShopOutletId, "9120013036924", null, null, null, null, null, 0);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchWithFiltersController.searchByMsku(shopOutletsRequestNoShopOutletId, mskuIds, null, null, null, null, 1, 20, true, 0);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchWithFiltersController.searchByNid(shopOutletsRequestNoShopOutletId, 16321232L, null, null, null, null, 1, 20, true, 0, null, null);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchWithFiltersController.searchByText(shopOutletsRequestNoShopOutletId, "мистраль", null, null, null, null, 16570407l, 1, 25, null, 0, null, null);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());
    }

    @Test
    void testAllShopOutLetsFromControllerNoShopOutletsNear() {
        search2xx("searchByBarcode?barcode=9120013036924&near=true", "SearchControllerTest.noOutletsNear.request1.json", "SearchControllerTest.noOutletsNear.response1.json");
        search2xx("searchByNid?nid=16321232&near=true", "SearchControllerTest.noOutletsNear.request1.json", "SearchControllerTest.noOutletsNear.response1.json");
        search2xx("searchByText?text=хлеб&near=true", "SearchControllerTest.noOutletsNear.request1.json", "SearchControllerTest.noOutletsNear.response1.json");
        search2xx("searchByMsku?msku=212823278&near=true", "SearchControllerTest.noOutletsNear.request1.json", "SearchControllerTest.noOutletsNear.response1.json");
        search2xx("searchPromo?near=true", "SearchControllerTest.noOutletsNear.request1.json", "SearchControllerTest.noOutletsNear.response1.json");
    }

    @Test
    void testAllShopOutLetsFromControlllerNoCoordsNested() {
        search4xx("searchByBarcode?barcode=9120013036924", "SearchControllerTest.noCoordsNested.request1.json");
        search4xx("searchByNid?nid=16321232", "SearchControllerTest.noCoordsNested.request1.json");
        search4xx("searchByText?text=хлеб", "SearchControllerTest.noCoordsNested.request1.json");
        search4xx("searchByMsku?msku=212823278", "SearchControllerTest.noCoordsNested.request1.json");
        search4xx("searchPromo", "SearchControllerTest.noCoordsNested.request1.json");
    }

    private void search4xx(String method, String requestBodyResourceName) {
        search(method, requestBodyResourceName, status().is4xxClientError());
    }

    @SneakyThrows
    private void search2xx(String method, String requestBodyResourceName, String responseBodyResourceName) {
        search(method, requestBodyResourceName, status().is2xxSuccessful())
            .andExpect(content().json(loadResourceAsString(responseBodyResourceName)));
    }

    @SneakyThrows
    private ResultActions search(String method, String requestBodyResourceName, ResultMatcher status) {
        return mockMvc.perform(post("/apiv1/product/" + method)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(loadResourceAsString(requestBodyResourceName)))
            .andDo(print())
            .andExpect(status);
    }

    @Test
    void testMiniOffers() throws Exception {
        final String barCode = "2100100003531";
        final String shopId = "574430";
        final String outletId = "88057809";
        final String reportResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportMiniOffersResponse.json");

        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", "yellow_msku")
                .withQueryStringParameter("text", String.format("barcode:%s", barCode))
                .withQueryStringParameter("fesh", shopId)
                .withQueryStringParameter("outlets", outletId)
            )
            .respond(
                HttpResponse.response(reportResponse)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
            );

        final String bffRequest = TestUtil.loadResourceAsString(
            "SearchControllerTest.bffRequestBody.json");
        final String bffResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.bffMiniOffersResponse.json");

        mockMvc.perform(MockMvcRequestBuilders
            .post("/apiv1/product/searchByBarcode")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(bffRequest)
            .param(Const.Param.BARCODE, barCode)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().json(bffResponse));
    }

    @Test
    void testPromoData() throws Exception {
        final String reportResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.testPromoData.reportResponse.json");

        // use some request body and set variables with identifiers from it
        final String bffRequestBody = TestUtil.loadResourceAsString(
            "SearchControllerTest.testVendorsFilterWithSearchPromo.bffRequestBody.json");

        final String shopId = "574430";
        final String outletId = "88057809";

        final String expectedBffResponse = TestUtil.loadResourceAsString(
            "SearchControllerTest.testPromoData.bffResponse.json");

        final String nid = "16321345";

        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", "yellow_promo")
                .withQueryStringParameter("nid", nid)
                .withQueryStringParameter("fesh", shopId)
                .withQueryStringParameter("outlets", outletId)
            )
            .respond(
                HttpResponse.response(reportResponse)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
            );
        mockMvc.perform(MockMvcRequestBuilders
            .post("/apiv1/product/searchPromo")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(bffRequestBody)
                .param("nid", nid)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().json(expectedBffResponse));
    }

}
