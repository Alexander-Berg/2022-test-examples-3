package ru.yandex.market.logistics.lom.controller.sender;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/sender/setup.xml")
class SenderControllerTest extends AbstractContextualTest {

    @Nonnull
    private static Stream<Arguments> searchSenderTestArguments() {
        return Stream.of(
            Arguments.of(
                "Выбор одиночного сендера",
                "controller/sender/request/search-single-sender.json",
                "controller/sender/response/search-single-sender.json"
            ),
            Arguments.of(
                "Выбор с несколькими сендерами",
                "controller/sender/request/search-multiple-senders.json",
                "controller/sender/response/search-multiple-senders.json"
            ),
            Arguments.of(
                "Комбинация сендер + статус не найдена",
                "controller/sender/request/search-not-found.json",
                "controller/sender/response/search-not-found.json"
            ),
            Arguments.of(
                "Пустой список сендеров",
                "controller/sender/request/search-empty-sender.json",
                "controller/sender/response/search-not-found.json"
            ),
            Arguments.of(
                "Пустой список статусов",
                "controller/sender/request/search-empty-statuses.json",
                "controller/sender/response/search-not-found.json"
            ),
            Arguments.of(
                "Пустые списки",
                "controller/sender/request/search-empty-lists.json",
                "controller/sender/response/search-not-found.json"
            )
        );
    }

    @DisplayName("Поиск сендеров по критериям")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchSenderTestArguments")
    void searchSenderTest(
        @SuppressWarnings("unused") String displayName,
        String request,
        String result
    ) throws Exception {
        mockMvc.perform(
            put("/senders/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(result));
    }

    @Nonnull
    private static Stream<Arguments> searchSenderInvalidRequestTestArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск без сендера",
                "controller/sender/request/search-wo-sender.json",
                "{\"message\":\"Following validation errors occurred:\\nField: 'senderIds', " +
                    "message: 'must not be null'\"}"
            ),
            Arguments.of(
                "Поиск без статусов сегментов",
                "controller/sender/request/search-wo-segment-statuses.json",
                "{\"message\":\"Following validation errors occurred:\\nField: 'haveWaybillSegmentStatuses', " +
                    "message: 'must not be null'\"}"
            ),
            Arguments.of(
                "Поиск без платформы",
                "controller/sender/request/search-wo-platform.json",
                "{\"message\":\"Following validation errors occurred:\\nField: 'platformClient', " +
                    "message: 'must not be null'\"}"
            )
        );
    }

    @DisplayName("Поиск сендеров по критериям (невалидный запрос)")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchSenderInvalidRequestTestArguments")
    void searchSenderTestInvalidRequest(
        @SuppressWarnings("unused") String displayName,
        String request,
        String response
    ) throws Exception {
        mockMvc.perform(
            put("/senders/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
        )
            .andExpect(status().is4xxClientError())
            .andExpect(content().json(response, false));
    }
}
