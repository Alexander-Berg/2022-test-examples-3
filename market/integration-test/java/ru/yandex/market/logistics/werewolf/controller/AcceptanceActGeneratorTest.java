package ru.yandex.market.logistics.werewolf.controller;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Генерация списка принятых заказов")
class AcceptanceActGeneratorTest extends AbstractRTAGeneratorTest {

    @ParameterizedTest
    @MethodSource
    @DisplayName("Успешная генерация АПП в формате HTML")
    void generateAcceptanceActSuccess(
        String requestPath,
        String responsePath
    ) throws Exception {
        performAndDispatch(
            requestPath,
            request -> request.accept(MediaType.TEXT_HTML)
        )
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent(responsePath)));
    }

    @Nonnull
    private static Stream<Arguments> generateAcceptanceActSuccess() {
        return Stream.of(
            Arguments.of(
                "controller/documents/request/RTA_single_order.json",
                "controller/documents/response/acceptance_act_single_order.html"
            ),
            Arguments.of(
                "controller/documents/request/RTA_three_orders.json",
                "controller/documents/response/acceptance_act_three_orders.html"
            ),
            Arguments.of(
                "controller/documents/request/RTA_null_partner_id.json",
                "controller/documents/response/acceptance_act_null_partner_id.html"
            )
        );
    }

    @Nonnull
    @Override
    protected String defaultHtmlResponseBodyPath() {
        return "controller/documents/response/acceptance_act_single_order.html";
    }

    @Nonnull
    @Override
    protected String requestPath() {
        return "/document/acceptanceAct/generate";
    }
}
