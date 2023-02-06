package ru.yandex.market.logistics.lom.controller.document;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.utils.TestUtils.validationErrorsJsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск актов приема-передачи")
public class AcceptanceCertificateControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("По идентификаторам заявок")
    @DatabaseSetup("/controller/document/before/acceptance_certificates.xml")
    void searchOk() throws Exception {
        searchAcceptanceCertificates(extractFileContent("controller/document/request/by_shipment_application_ids.json"))
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    extractFileContent("controller/document/response/by_shipment_application_ids.json"),
                    true
                )
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация запроса")
    @MethodSource("validateRequestArguments")
    void validateRequest(String field, String message, String request) throws Exception {
        searchAcceptanceCertificates(request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorsJsonContent(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Triple.of(
                "shipmentApplicationIds",
                "must not be empty",
                "{\"shipmentApplicationIds\":null}"
            ),
            Triple.of(
                "shipmentApplicationIds",
                "must not be empty",
                "{\"shipmentApplicationIds\":[]}"
            ),
            Triple.of(
                "shipmentApplicationIds[]",
                "must not be null",
                "{\"shipmentApplicationIds\":[null]}"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Получить пустой список")
    void searchOkEmpty() throws Exception {
        searchAcceptanceCertificates(extractFileContent("controller/document/request/by_shipment_application_ids.json"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @NotNull
    private ResultActions searchAcceptanceCertificates(@Nonnull String request) throws Exception {
        return mockMvc.perform(
            put("/acceptance-certificates/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        );
    }
}
