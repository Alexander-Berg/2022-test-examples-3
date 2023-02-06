package ru.yandex.market.fmcg.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.yandex.market.fmcg.bff.controller.dto.MarketProductDto;
import ru.yandex.market.fmcg.bff.controller.dto.PriceDto;
import ru.yandex.market.fmcg.bff.controller.dto.ResultsResponseV2;
import ru.yandex.market.fmcg.bff.controller.dto.SearchResultDto;
import ru.yandex.market.fmcg.bff.controller.dto.ShopOutlet;
import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.fmcg.bff.test.MockServerUtil;
import ru.yandex.market.fmcg.bff.test.TestUtil;
import ru.yandex.market.fmcg.client.backend.FmcgBackClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class SearchControllerTest extends FmcgBffTest {
    private List<ShopOutlet> shopOutlets2;
    private List<Long> mskuIds;

    @Autowired
    FmcgBackClient fmcgBackClient;

    @Autowired
    private SearchController searchController;
    private SearchResultDto resultDto;

    @BeforeEach
    void setUp() {
        shopOutlets2 = new LinkedList<>();
        shopOutlets2.add(new ShopOutlet(1L, 2334L));
        shopOutlets2.add(new ShopOutlet(3242L, null));
        mskuIds = new LinkedList<>();
        mskuIds.add(212823278L);
        resultDto = getResultDto();
    }

    @BeforeEach
    void initServer() {
        MockServerUtil.INSTANCE.reset();
    }

    private SearchResultDto getResultDto() {
        resultDto = new SearchResultDto();
        resultDto.setDocumentCount(1);
        resultDto.setPager(new SearchResultDto.PagerDto(0, 20, 1));
        List<SearchResultDto.ItemAvailabilityDto> itemAvailabilityDto = Collections.singletonList(new SearchResultDto.ItemAvailabilityDto(
            "574430", "880", false,
            null, null, null, new ArrayList<>()));
        SearchResultDto.CategoryDto categoryDto = new SearchResultDto.CategoryDto("Снэки", 15714670L, 16570646L, false);
        SearchResultDto.CategoryDto leafCategory = new SearchResultDto.CategoryDto("Чипсы", 15714671L, 16570648L, false);
        resultDto.setItems(Collections.singletonList(
            new SearchResultDto.ItemDto("100464699111", 4390L, 4390L, null, itemAvailabilityDto,
                categoryDto, leafCategory, null, null, null)));
        resultDto.setCategories(Collections.singletonList(leafCategory));
        resultDto.setFoundAlcohol("Some");
        return resultDto;
    }

    void setSearchTestData(String request, String place, String response) {
        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request(request).withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", place))
            .respond(
                HttpResponse.response(response)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json; charset=utf-8")
            );
    }

    void setSearchTestData(String place, List<Parameter> parameters, String response) {
        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withMethod("GET").withPath("/yandsearch")
                .withQueryStringParameter("place", place)
                .withQueryStringParameters(parameters)
            )
            .respond(
                HttpResponse.response(response)
                    .withStatusCode(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json; charset=utf-8")
            );
    }


    void setupSearchMockServerResponse(String request, String place, String loadResponse) {
        setSearchTestData(request, place, loadResponse);
    }

    @Test
    void testSearchWithAdultParamWithAlcoholSome() {
        List<ShopOutlet> shopOutlets = Collections.singletonList(
            new ShopOutlet(574430L, 880L)
        );
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswer.json");
        setupSearchMockServerResponse("/apiv1/product/searchByText", "yellow_msku", loadResponse);
        ResultsResponseV2 resultByText = searchController.searchByText(
            shopOutlets, "4602112203612", 435L,
            0, 20, 0, null);
        initServer();
        setupSearchMockServerResponse("/apiv1/product/searchByMsku", "yellow_msku", loadResponse);
        ResultsResponseV2 resultsByMsku = searchController.searchByMsku(shopOutlets,
            mskuIds, 0, 20, 1);
        Assertions.assertFalse(resultByText.getCollections().isEmpty());
        assertEquals(resultDto, resultByText.getCollections().get(SearchResultDto.ENTITY).iterator().next());
        assertEquals(resultDto, resultsByMsku.getCollections().get(SearchResultDto.ENTITY).iterator().next());
    }

    @Test
    void testSearchWithHasAlcoOfferParam() {
        List<ShopOutlet> shopOutlets = Arrays.asList(
            new ShopOutlet(574430L, 880L),
            new ShopOutlet(57440L, 43534L)
        );
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswer.json");
        setupSearchMockServerResponse("/apiv1/product/searchByText", "yellow_msku", loadResponse);
        ResultsResponseV2 resultByText = searchController.searchByText(
            shopOutlets, "4602112203612", null, 0, 20, 0, null);
        initServer();
        setupSearchMockServerResponse("/apiv1/product/searchByMsku", "yellow_msku", loadResponse);
        ResultsResponseV2 resultsByMsku = searchController.searchByMsku(shopOutlets,
            mskuIds,  0, 20, 1);
        initServer();
        setupSearchMockServerResponse("/apiv1/product/searchPromos", "yellow_promo", loadResponse);
        ResultsResponseV2 resultByPromo = searchController.searchPromos(shopOutlets,
            234234L, 0, 20, 1);
        initServer();
        setupSearchMockServerResponse("/apiv1/product/searchByNid", "yellow_msku", loadResponse);
        ResultsResponseV2 resultByNid = searchController.searchByNid(shopOutlets,
            234234L, 0, 20,1);
        initServer();
        setupSearchMockServerResponse("/apiv1/product/searchByBarcode", "yellow_msku", loadResponse);
        ResultsResponseV2 resultByBarcode = searchController.searchByBarcode(shopOutlets,
            "4235234523", 1);
        org.assertj.core.api.Assertions.assertThat(resultByText.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        org.assertj.core.api.Assertions.assertThat(resultByBarcode.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        org.assertj.core.api.Assertions.assertThat(resultByNid.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        org.assertj.core.api.Assertions.assertThat(resultByPromo.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
        org.assertj.core.api.Assertions.assertThat(resultsByMsku.getCollections().get(MarketProductDto.ENTITY).iterator().next()).
            extracting("hasAlcoOffer").isEqualTo(Collections.singletonList(true));
    }

    @Test
    void testProductDetails() {
        List<ShopOutlet> shopOutlets = Arrays.asList(
            new ShopOutlet(578170L,99934336L),
            new ShopOutlet(574431L, 96690354L)
        );
        final Long mskuId = 100767162863L;
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerProductDetails.1.json");
        setSearchTestData(
            "yellow_product_details",
            Arrays.asList(
                Parameter.param("fesh", "578170,574431"),
                Parameter.param("outlets", "99934336,96690354"),
                Parameter.param("market-sku", String.valueOf(mskuId))
            ),
            loadResponse
        );
        ResultsResponseV2 result = searchController.productDetails(shopOutlets, mskuId);
        org.assertj.core.api.Assertions.assertThat(result.getCollections().get(PriceDto.ENTITY).iterator().next()).
            extracting("product").isEqualTo(Collections.singletonList("100767162863"));
        org.assertj.core.api.Assertions.assertThat(result.getCollections().get(PriceDto.ENTITY).iterator().next()).
            extracting("shop").isEqualTo(Collections.singletonList("578170"));
        org.assertj.core.api.Assertions.assertThat(result.getCollections().get(PriceDto.ENTITY).iterator().next()).
            extracting("outlet").isEqualTo(Collections.singletonList("97305806"));
        org.assertj.core.api.Assertions.assertThat(result.getCollections().get(PriceDto.ENTITY).iterator().next()).
            extracting("price").isEqualTo(Collections.singletonList(5270L));
        org.assertj.core.api.Assertions.assertThat(result.getCollections().get(PriceDto.ENTITY).iterator().next()).
            extracting("promos").isEqualTo(Collections.singletonList(
                new SearchResultDto.PromoDto(
                    "n-plus-m",
                    "2019-08-26T21:00:00Z",  "2019-09-16T20:59:59Z",
                    "СПЕЦПРЕДЛОЖЕНИЕ: 2 ПО ЦЕНЕ 1",
                    false,
                    "2 + 1",
                    null,
                    "27 августа - 16 сентября",
                    null,
                     new SearchResultDto.NPlusMDto(2, 1),
                    null
                )
            )
        );
        org.assertj.core.api.Assertions.assertThat(((List)(result.getCollections().get(PriceDto.ENTITY))).get(1)).
            extracting("product").isEqualTo(Collections.singletonList("100767162863"));
        org.assertj.core.api.Assertions.assertThat(((List)(result.getCollections().get(PriceDto.ENTITY))).get(1)).
            extracting("shop").isEqualTo(Collections.singletonList("583103"));
        org.assertj.core.api.Assertions.assertThat(((List)(result.getCollections().get(PriceDto.ENTITY))).get(1)).
            extracting("outlet").isEqualTo(Collections.singletonList("102961891"));
        org.assertj.core.api.Assertions.assertThat(((List)(result.getCollections().get(PriceDto.ENTITY))).get(1)).
            extracting("price").isEqualTo(Collections.singletonList(4899L));
        org.assertj.core.api.Assertions.assertThat(((List)(result.getCollections().get(PriceDto.ENTITY))).get(1)).
            extracting("promos").isEqualTo(Collections.singletonList(null));
    }


    @Test
    void testNoProductDetails() {
        List<ShopOutlet> shopOutlets = Arrays.asList(
            new ShopOutlet(578170L,99934336L)
        );
        final Long mskuId = 100767162863L;
        String loadResponse = TestUtil.loadResourceAsString("SearchControllerTest.ReportAnswerProductDetails.2.json");
        setSearchTestData(
            "yellow_product_details",
            Arrays.asList(
                Parameter.param("fesh", "578170"),
                Parameter.param("outlets", "99934336"),
                Parameter.param("market-sku", String.valueOf(mskuId))
            ),
            loadResponse
        );
        ResultsResponseV2 result = searchController.productDetails(shopOutlets, mskuId);
        org.assertj.core.api.Assertions.assertThat(result.getCollections().size())
            .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(result.getCollections().get(PriceDto.ENTITY).size())
            .isEqualTo(0);
    }

    @Test
    void testAllShopOutLetsFromController() {
        IllegalArgumentException exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchController.searchPromos(shopOutlets2, 16321962L, 1, 20, 0);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchController.searchByBarcode(shopOutlets2, "9120013036924", 0);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchController.searchByMsku(shopOutlets2, mskuIds, 1, 20, 0);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());

        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchController.searchByNid(shopOutlets2, 16321232L, 1, 20, 0);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());
        exeption = assertThrows(IllegalArgumentException.class, () -> {
            searchController.searchByText(shopOutlets2, "мистраль", 16570407L, 1, 25, 0, null);
        });
        assertEquals("No shopOutlet number", exeption.getMessage());
    }

}
