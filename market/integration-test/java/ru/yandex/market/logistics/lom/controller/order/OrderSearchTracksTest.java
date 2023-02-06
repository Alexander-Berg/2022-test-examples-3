package ru.yandex.market.logistics.lom.controller.order;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.filter.OrderTracksFilter;

import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
class OrderSearchTracksTest extends AbstractContextualTest {

    @MethodSource("searchArgumentOk")
    @DisplayName("Получить трэки заказов (успех)")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/controller/order/tracks/before/search_orders_tracks.xml")
    void searchOrdersTracksOk(
        String displayName,
        OrderTracksFilter request,
        String response
    ) throws Exception {
        searchOrdersTracks(request)
            .andExpect(status().isOk())
            .andExpect(jsonContent(response));
    }

    @Nonnull
    private static Stream<Arguments> searchArgumentOk() {
        return Stream.of(
            Arguments.of(
                "По внешнему идентификатору заказа",
                OrderTracksFilter.builder()
                    .platformClientId(3L)
                    .externalIds(List.of("103"))
                    .build(),
                "controller/order/tracks/response/3.json"
            ),
            Arguments.of(
                "По нескольким внешнем идентификаторам заказа",
                OrderTracksFilter.builder()
                    .platformClientId(3L)
                    .externalIds(List.of("101", "102"))
                    .build(),
                "controller/order/tracks/response/1_and_2.json"
            ),
            Arguments.of(
                "По отсутствующему внешнему идентификатору",
                OrderTracksFilter.builder()
                    .platformClientId(3L)
                    .externalIds(List.of("some-invalid-external-id-that-must-be-ignored"))
                    .build(),
                "controller/order/tracks/response/empty.json"
            ),
            Arguments.of(
                "По пустым спискам",
                OrderTracksFilter.builder()
                    .platformClientId(3L)
                    .externalIds(List.of())
                    .build(),
                "controller/order/tracks/response/empty.json"
            )
        );
    }

    @MethodSource("searchArgumentError")
    @DisplayName("Получить трэки заказов (ошибка)")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/controller/order/tracks/before/search_orders_tracks.xml")
    void searchOrdersTracksError(
        String displayName,
        OrderTracksFilter request,
        ResultMatcher response
    ) throws Exception {
        searchOrdersTracks(request)
            .andExpect(status().isBadRequest())
            .andExpect(response);
    }

    @Nonnull
    private static Stream<Arguments> searchArgumentError() {
        return Stream.of(
            Arguments.of(
                "Поиск без платформы",
                OrderTracksFilter.builder()
                    .externalIds(List.of("103"))
                    .build(),
                fieldValidationErrorMatcher("platformClientId", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Поиск без внешнего идентификатора",
                OrderTracksFilter.builder()
                    .platformClientId(3L)
                    .build(),
                fieldValidationErrorMatcher("externalIds", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Поиск с null externalId",
                OrderTracksFilter.builder()
                    .platformClientId(3L)
                    .externalIds(Collections.singletonList(null))
                    .build(),
                fieldValidationErrorMatcher("externalIds[0]", NOT_NULL_ERROR_MESSAGE)
            )
        );
    }

    @MethodSource("searchArgumentSize")
    @DisplayName("Получить трэки заказов (проверка ограничения размера)")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/controller/order/tracks/before/search_orders_tracks.xml")
    void searchOrdersTracksSize(
        String displayName,
        Long externalIdsListSize,
        ResultMatcher response,
        ResultMatcher status
    ) throws Exception {
        searchOrdersTracks(generateExternalIdsList(externalIdsListSize))
            .andExpect(status)
            .andExpect(response);
    }

    @Nonnull
    private static Stream<Arguments> searchArgumentSize() {
        return Stream.of(
            Arguments.of(
                "Поиск со списком externalId максимальной разрешённой длины",
                (long) Short.MAX_VALUE - 1,
                jsonContent("controller/order/tracks/response/1_and_2_and_3.json"),
                status().isOk()
            ),
            Arguments.of(
                "Слишком большой список externalId",
                (long) Short.MAX_VALUE,
                fieldValidationErrorMatcher("externalIds", "size must be between 0 and 32766"),
                status().isBadRequest()
            )
        );
    }

    @NotNull
    private ResultActions searchOrdersTracks(OrderTracksFilter request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/tracks/search", request));
    }

    @NotNull
    private OrderTracksFilter generateExternalIdsList(Long size) {
        List<String> externalIds = LongStream.range(1, size + 1)
            .mapToObj(String::valueOf)
            .collect(Collectors.toList());
        return OrderTracksFilter.builder().externalIds(externalIds).platformClientId(3L).build();
    }
}
