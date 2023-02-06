package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.filter.ExistingOrderSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ExistingOrderSearchFilter.ExistingOrderSearchFilterBuilder;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение информации о наличии заказов")
class OrderCheckExistingTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("checkArgument")
    @DisplayName("Проверка наличия заказов")
    @DatabaseSetup("/controller/order/search/orders.xml")
    void checkOrders(
        String displayName,
        ExistingOrderSearchFilterBuilder request,
        String response
    ) throws Exception {
        checkOrders(request)
            .andExpect(status().isOk())
            .andExpect(jsonContent(response));
    }

    @Nonnull
    private static Stream<Arguments> checkArgument() {
        return Stream.of(
            Arguments.of(
                "Проверка заказов",
                ExistingOrderSearchFilter.builder()
                    .partnerIds(List.of(1L, 2L, 3L, 4L, 5L, 6L))
                    .createdFrom(Instant.parse("2019-06-01T12:00:00Z")),
                "controller/order/search/response/shops_with_orders.json"
            ),
            Arguments.of(
                "Проверка отсутствия заказов после указанной даты",
                ExistingOrderSearchFilter.builder()
                    .partnerIds(List.of(1L, 2L, 3L, 4L, 5L, 6L))
                    .createdFrom(Instant.parse("2019-07-01T12:00:00Z")),
                "controller/order/search/response/shops_without_orders.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("checkValidation")
    @DisplayName("Валидация фильтра проверки заказов")
    void checkOrdersValidation(
        String displayName,
        ExistingOrderSearchFilterBuilder request,
        String message
    ) throws Exception {
        checkOrders(request)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Following validation errors occurred:\n" + message));
    }

    @Nonnull
    private static Stream<Arguments> checkValidation() {
        return Stream.of(
            Arguments.of(
                "Пустой список магазинов",
                ExistingOrderSearchFilter.builder()
                    .partnerIds(List.of())
                    .createdFrom(Instant.parse("2019-06-01T12:00:00Z")),
                "Field: 'partnerIds', message: 'must not be empty'"
            ),
            Arguments.of(
                "Null-value в дате создания",
                ExistingOrderSearchFilter.builder()
                    .partnerIds(List.of(1L))
                    .createdFrom(null),
                "Field: 'createdFrom', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-value в списке магазинов",
                ExistingOrderSearchFilter.builder()
                    .partnerIds(Collections.singletonList(null))
                    .createdFrom(Instant.parse("2019-06-01T12:00:00Z")),
                "Field: 'partnerIds[0]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-value в списке магазинов",
                ExistingOrderSearchFilter.builder()
                    .partnerIds(null)
                    .createdFrom(Instant.parse("2019-06-01T12:00:00Z")),
                "Field: 'partnerIds', message: 'must not be empty'"
            )
        );
    }

    @Nonnull
    private ResultActions checkOrders(ExistingOrderSearchFilterBuilder request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/existing", request.build()));
    }
}
