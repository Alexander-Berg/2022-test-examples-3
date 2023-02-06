package ru.yandex.market.logistics.tarifficator.controller;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.MUST_BE_GREATER_THAN_0;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.MUST_NOT_BE_NULL;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест контроллера DeliveryOptionController")
class DeliveryOptionControllerTest extends AbstractContextualTest {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Получение опций доставки (положительный сценарий)")
    @DatabaseSetup(
        {
            "/tags/tags.xml",
            "/tariffs/courier_without_active_price_lists_1.xml",
        }
    )
    @DatabaseSetup(
        type = DatabaseOperation.INSERT,
        value = {
            "/tariffs/pick_up_100.xml",
            "/tariffs/post_200.xml",
            "/tariffs/post_400_big_dimension_weight.xml",
            "/tariffs/post_700_different_tag.xml",
            "/tariffs/post_800_multiple_tags.xml",
        }
    )
    @MethodSource("validRequestProvider")
    void getTariffs(
        @SuppressWarnings("unused") String caseName,
        String requestFilePath,
        String responseFilePath
    ) throws Exception {
        performDeliveryOptionsSearch(requestFilePath)
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(responseFilePath)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Запрос с невалидными входными данными")
    @MethodSource("invalidRequestProvider")
    void createInvalidTariff(
        @SuppressWarnings("unused") String caseName,
        String requestFilePath,
        String fieldName,
        String fieldError
    ) throws Exception {
        performDeliveryOptionsSearch(requestFilePath)
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationError(fieldName, fieldError));
    }

    private ResultActions performDeliveryOptionsSearch(String requestFilePath) throws Exception {
        return mockMvc.perform(
            put("/delivery-options/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }

    private static Stream<Arguments> validRequestProvider() {
        return Stream.of(
            Arguments.of(
                "Поиск тарифов без тэгов",
                "controller/delivery-options/request/get_options.json",
                "controller/delivery-options/response/get_options.json"
            ),
            Arguments.of(
                "Поиск тарифов по одному тэгу",
                "controller/delivery-options/request/get_options_by_tag.json",
                "controller/delivery-options/response/get_options_by_tag.json"
            ),
            Arguments.of(
                "Поиск тарифов по нескольким тэгам",
                "controller/delivery-options/request/get_options_by_multiple_tags.json",
                "controller/delivery-options/response/get_options_by_multiple_tags.json"
            )
        );
    }

    private static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            Arguments.of(
                "Указаны некорректные идентификаторы тарифов",
                "controller/delivery-options/request/get_options_invalid_tariffs.json",
                "tariffIds[]",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Не указан вес",
                "controller/delivery-options/request/get_options_empty_weight.json",
                "weight",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Не указана ширина",
                "controller/delivery-options/request/get_options_empty_width.json",
                "width",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Не указана длина",
                "controller/delivery-options/request/get_options_empty_length.json",
                "length",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Не указана высота",
                "controller/delivery-options/request/get_options_empty_height.json",
                "height",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Не указана локация отправления",
                "controller/delivery-options/request/get_options_empty_location_from.json",
                "locationFrom",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Не указана локация назначения",
                "controller/delivery-options/request/get_options_empty_location_to.json",
                "locationTo",
                MUST_NOT_BE_NULL
            ),
            Arguments.of(
                "Указан отрицательный вес",
                "controller/delivery-options/request/get_options_negative_weight.json",
                "weight",
                MUST_BE_GREATER_THAN_0
            ),
            Arguments.of(
                "Указана отрицательная длина",
                "controller/delivery-options/request/get_options_negative_length.json",
                "length",
                MUST_BE_GREATER_THAN_0
            ),
            Arguments.of(
                "Указана отрицательная ширина",
                "controller/delivery-options/request/get_options_negative_width.json",
                "width",
                MUST_BE_GREATER_THAN_0
            ),
            Arguments.of(
                "Указана отрицательная высота",
                "controller/delivery-options/request/get_options_negative_height.json",
                "height",
                MUST_BE_GREATER_THAN_0
            )
        );
    }

}
