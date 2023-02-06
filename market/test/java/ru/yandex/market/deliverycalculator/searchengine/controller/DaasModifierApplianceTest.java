package ru.yandex.market.deliverycalculator.searchengine.controller;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.service.FeedParserWorkflowService;
import ru.yandex.market.deliverycalculator.test.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "classpath:modifiers/db/setup_modifiers_db.csv")
class DaasModifierApplianceTest extends FunctionalTest {

    @Autowired
    private FeedParserWorkflowService feedParserWorkflowService;

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("modifiersProvider")
    void modifierApplyTest(@SuppressWarnings("unused") String caseName, String request, String response)
            throws Exception {
        feedParserWorkflowService.updateActiveGenerationId();
        feedParserWorkflowService.importGenerations();
        deliverySearchRequest(request)
            .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(TestUtils.extractFileContent("modifiers/response/" + response)));
    }

    @Nonnull
    private static Stream<Arguments> modifiersProvider() {
        return Stream.of(
                Arguments.of("ok test", "ok_request.json", "ok_response.json"),
                Arguments.of("Options not found", "not_found_option_request.json", "empty_response.json"),
                Arguments.of("No sender request", "no_sender_request.json", "no_modified_response.json"),
                Arguments.of("Not exist sender request", "not_exist_sender_request.json", "no_modified_response.json"),
                Arguments.of("No delivery services request", "no_delivery_services_request.json", "ok_response.json"),
                Arguments.of(
                        "Modifiers not applicable",
                        "modifiers_not_applicable_request.json",
                        "only_post_modified_response.json"
                ),
                Arguments.of(
                        "No collaborating deliveries",
                        "no_collaborating_deliveries_request.json",
                        "empty_response.json"
                ),
                Arguments.of("Modify cost", "cost_modify_request.json", "cost_modify_response.json"),
                Arguments.of("Composite modify", "composite_modify_request.json", "composite_modify_response.json"),
                Arguments.of("Modify with priority", "priority_modify_request.json", "priority_modify_response.json"),
                Arguments.of("Modify time", "time_modify_request.json", "time_modify_response.json"),
                Arguments.of(
                        "Modify available delivery",
                        "carrier_available_modify_request.json",
                        "empty_response.json"
                ),
                Arguments.of("Modify services", "services_modify_request.json", "services_modify_response.json"),
                Arguments.of("Modify unknown", "unknown_modify_request.json", "empty_response.json"),
                Arguments.of("Modify empty", "no_modifiers_request.json", "no_modified_response.json"),
                Arguments.of(
                        "Modify two services",
                        "two_services_modify_request.json",
                        "two_services_modify_response.json"
                ),
                Arguments.of("Without delivery services", "no_delivery_services.json", "ok_response.json")
        );

    }

    @Nonnull
    private ResultActions deliverySearchRequest(String requestPath) throws Exception {
        final MvcResult result = mockMvc.perform(
                post("/daas/deliverySearch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.extractFileContent("modifiers/request/" + requestPath)))
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result));
    }
}
