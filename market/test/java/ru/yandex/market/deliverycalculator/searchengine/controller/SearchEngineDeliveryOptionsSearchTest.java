package ru.yandex.market.deliverycalculator.searchengine.controller;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.service.FeedParserWorkflowService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Функциональный тест поиска опций доставки DAAS через SearchEngineController")
class SearchEngineDeliveryOptionsSearchTest extends FunctionalTest {

    @Autowired
    private FeedParserWorkflowService feedParserWorkflowService;

    @BeforeEach
    void before() {
        feedParserWorkflowService.updateActiveGenerationId();
        feedParserWorkflowService.importGenerations();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchTestProvider")
    @DisplayName("Успешный поиск опций доставки")
    @DbUnitDataSet(before = {
        "classpath:daas/search-delivery-options/db/common_data.csv",
        "classpath:daas/search-delivery-options/db/yado_settings.csv"
    })
    void searchDeliveryOptions(@SuppressWarnings("unused") String caseName, String filePath) throws Exception {
        deliverySearchAsyncRequest(filePath)
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(response(filePath));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchTestProvider")
    @DisplayName("Успешный поиск опций доставки, в мете находится маппинг СД к регионам")
    @DbUnitDataSet(before = {
        "classpath:daas/search-delivery-options/db/common_data.csv",
        "classpath:daas/search-delivery-options/db/yado_settings_as_mapping.csv"
    })
    void searchDeliveryOptionsAnotherMeta(
        @SuppressWarnings("unused") String caseName,
        String filePath
    ) throws Exception {
        deliverySearchAsyncRequest(filePath)
            .andExpect(status().isOk())
            .andDo(print())
            .andExpect(response(filePath));
    }

    @Test
    @DisplayName("Успешный поиск в другом регионе по подключенной службе")
    @DbUnitDataSet(before = {
        "classpath:daas/search-delivery-options/db/common_data.csv",
        "classpath:daas/search-delivery-options/db/yado_settings_several_regions.csv"
    })
    void searchDeliveryOptionsSeveralRegions() throws Exception {
        deliverySearchAsyncRequest("search_from_another_region.json")
            .andExpect(status().isOk())
            .andDo(print())
            .andExpect(response("search_from_another_region.json"));
    }

    @Test
    @DisplayName("Ошибки валидации входных данных")
    void searchValidationErrors() throws Exception {
        deliverySearchRequest("search_validation_errors.json")
                .andExpect(status().isBadRequest())
                .andExpect(response("search_validation_errors.json"));
    }

    @Nonnull
    private static Stream<Arguments> searchTestProvider() {
        return Stream.of(
                Arguments.of("Поиск опций доставки по всем тарифам", "search_all_types.json"),
                Arguments.of("Поиск опций доставки по всем тарифам с индексом", "search_all_types_with_postal_code.json"),
                Arguments.of("Поиск опций доставки ПВЗ", "search_pickup.json"),
                Arguments.of("Поиск опций доставки Почты РФ", "search_post.json"),
                Arguments.of("Поиск опций доставки ПВЗ с фильтром по точке", "search_pickup_filter_by_pickpoint_id.json"),
                Arguments.of("Поиск опций доставки Почты РФ с фильтром по индексу", "search_post_filter_by_postal_code.json"),
                Arguments.of("Поиск опций доставки Почты РФ с фильтром по несуществующему индексу", "search_post_filter_by_incorrect_postal_code.json"),
                Arguments.of("Поиск опций доставки с явно указанным тарифом", "search_all_filter_by_tariff_id.json"),
                Arguments.of("Поиск опций доставки по всем тарифам с указанием senderId", "search_all_types_by_sender_id_and_offer_price.json"),
                Arguments.of("Поиск опций доставки не из своего региона", "search_from_incorrect_region.json")
        );
    }

    private ResultActions deliverySearchAsyncRequest(String requestPath) throws Exception {
        return mockMvc.perform(
                asyncDispatch(
                        deliverySearchRequest(requestPath)
                                .andExpect(request().asyncStarted())
                                .andReturn()
                ));
    }

    private ResultActions deliverySearchRequest(String requestPath) throws Exception {
        return mockMvc.perform(
                post("/daas/deliverySearch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(StringTestUtil.getString(
                                SearchEngineDeliveryOptionsSearchTest.class,
                                "/daas/search-delivery-options/request/" + requestPath
                        ))
        );
    }

    private static ResultMatcher response(String responsePath) {
        return content().json(StringTestUtil.getString(
                SearchEngineDeliveryOptionsSearchTest.class,
                "/daas/search-delivery-options/response/" + responsePath
        ));
    }
}
