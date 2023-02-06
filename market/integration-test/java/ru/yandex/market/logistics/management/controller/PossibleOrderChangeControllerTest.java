package ru.yandex.market.logistics.management.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanDatabase
class PossibleOrderChangeControllerTest extends AbstractContextualTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-01-20T20:20:20Z");

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_with_POCh.xml")
    void testGetPOChGroup() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/possible-order-changes")
            .param("types", "ORDER_ITEMS"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/possible_order_changes/POCh_group.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_with_POCh.xml")
    void testGetPOCh() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/possible-order-changes/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/possible_order_changes/POCh.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getPageSource")
    @DisplayName("Получение страницы")
    @DatabaseSetup("/data/controller/possible_order_changes/partners_with_POCh.xml")
    void testGetPossibleOrderChangesPage(
        @SuppressWarnings("unused") String name,
        String pageNumber,
        String size,
        List<String> types,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/externalApi/partners/possible-order-changes/page")
                .param("page", pageNumber)
                .param("size", size)
                .param("types", types.toArray(String[]::new))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(responsePath));
    }

    private static Stream<Arguments> getPageSource() {
        return Stream.of(
            Arguments.of(
                "Первая страница",
                "0",
                "2",
                List.of("ORDER_ITEMS"),
                "data/controller/possible_order_changes/POCh_page_0.json"
            ),
            Arguments.of(
                "Вторая страница",
                "1",
                "2",
                List.of("ORDER_ITEMS"),
                "data/controller/possible_order_changes/POCh_page_1.json"
            ),
            Arguments.of(
                "Последняя страница",
                "3",
                "4",
                List.of("ORDER_ITEMS", "DELIVERY_DATES", "RECIPIENT"),
                "data/controller/possible_order_changes/POCh_page_last.json"
            ),
            Arguments.of(
                "После последней страницы - пустая страница",
                "4",
                "4",
                List.of("ORDER_ITEMS", "DELIVERY_DATES", "RECIPIENT"),
                "data/controller/possible_order_changes/POCh_page_after_last.json"
            )
        );
    }

    @Test
    @DisplayName("Получение страницы без указания параметра types")
    @DatabaseSetup("/data/controller/possible_order_changes/partners_with_POCh.xml")
    void testGetPossibleOrderChangesPage() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/externalApi/partners/possible-order-changes/page")
                .param("page", "0")
                .param("size", "1")
        )
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getPageInvalidPageableParamsSource")
    @DisplayName("Неправильные параметры страницы меняются на дефолтные")
    void testGetPossibleOrderChangesPageInvalidPageableParams(
        @SuppressWarnings("unused") String name,
        String pageNumber,
        String size,
        List<String> types
    ) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/externalApi/partners/possible-order-changes/page")
                .param("pageNumber", pageNumber)
                .param("size", size)
                .param("types", types.toArray(String[]::new))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/possible_order_changes/POCh_empty_page_default_pageable.json"
            ));
    }

    private static Stream<Arguments> getPageInvalidPageableParamsSource() {
        return Stream.of(
            Arguments.of(
                "Отрицательная страница",
                "-1",
                "20",
                List.of("ORDER_ITEMS")
            ),
            Arguments.of(
                "Отрицательный размер",
                "0",
                "-2",
                List.of("ORDER_ITEMS")
            ),
            Arguments.of(
                "Размер равен нулю",
                "0",
                "0",
                List.of("ORDER_ITEMS")
            )
        );
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_wo_POCh.xml")
    @ExpectedDatabase(
        value = "/data/controller/possible_order_changes/partner_with_POCh.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreatePOCh() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/possible-order-changes")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil
                .pathToJson("data/controller/possible_order_changes/POCh_create_request.json")
            )
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/possible_order_changes/POCh.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_wo_POCh.xml")
    @ExpectedDatabase(
        value = "/data/controller/possible_order_changes/partner_with_multiple_POCh.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateMultiplePOChs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/possible-order-changes/multiple")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil
                .pathToJson("data/controller/possible_order_changes/POCh_create_multiple_request.json")
            )
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/possible_order_changes/POCh_multiple.json"));
    }

    @ParameterizedTest
    @MethodSource("getInvalidCreateRequests")
    @DatabaseSetup("/data/controller/possible_order_changes/partner_wo_POCh.xml")
    @ExpectedDatabase(
        value = "/data/controller/possible_order_changes/partner_wo_POCh.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateInvalidPOCh(String requestPath, String missingFieldName) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/possible-order-changes")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.pathToJson(requestPath)))
            .andExpect(status().is4xxClientError())
            .andExpect(content().json(
                "{\"errors\":[{\"defaultMessage\":\"Обязательно для заполнения\",\"field\":\"" +
                    missingFieldName +
                    "\"}]}",
                false
            ));
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_wo_POCh.xml")
    @ExpectedDatabase(
        value = "/data/controller/possible_order_changes/partner_wo_POCh.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreatePOChForNonExistingPartner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/possible-order-changes")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.pathToJson(
                "data/controller/possible_order_changes/wrong_partner_POCh_request.json"
            ))
        )
            .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_wo_POCh.xml")
    @ExpectedDatabase(
        value = "/data/controller/possible_order_changes/partner_with_POCh_wo_cp_from.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreate1rdPartyPOChWithoutCheckpointFrom() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/possible-order-changes")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.pathToJson("data/controller/possible_order_changes/1st_party_POCh_wo_from_request.json")))
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/data/controller/possible_order_changes/partner_wo_POCh.xml")
    @ExpectedDatabase(
        value = "/data/controller/possible_order_changes/partner_wo_POCh.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreate3rdPartyPOChWithoutCheckpointFrom() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/possible-order-changes")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TestUtil.pathToJson("data/controller/possible_order_changes/3rd_party_POCh_wo_from_request.json")))
            .andExpect(status().is4xxClientError());
    }

    private static Stream<Arguments> getInvalidCreateRequests() {
        return Stream.of(
            Arguments.of("data/controller/possible_order_changes/POCh_request_wo_cp_to.json", "checkpointStatusTo"),
            Arguments.of("data/controller/possible_order_changes/POCh_request_wo_enabled.json", "enabled"),
            Arguments.of("data/controller/possible_order_changes/POCh_request_wo_method.json", "method"),
            Arguments.of("data/controller/possible_order_changes/POCh_request_wo_partner.json", "partnerId"),
            Arguments.of("data/controller/possible_order_changes/POCh_request_wo_type.json", "type")
        );
    }
}
