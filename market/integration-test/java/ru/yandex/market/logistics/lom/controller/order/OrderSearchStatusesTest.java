package ru.yandex.market.logistics.lom.controller.order;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.OrderIdDto;
import ru.yandex.market.logistics.lom.model.filter.OrderStatusFilter;
import ru.yandex.market.logistics.lom.model.filter.OrderStatusFilter.OrderStatusFilterBuilder;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск истории статусов заказов")
class OrderSearchStatusesTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("filterValidationSource")
    @DisplayName("Валидация фильтра")
    void filterValidation(
        String message,
        UnaryOperator<OrderStatusFilterBuilder> filterModifier
    ) throws Exception {
        searchOrdersStatuses(filterModifier.apply(minimalFilterBuilder()).build())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Following validation errors occurred:\n" +
                    message
            ));
    }

    @Nonnull
    private static Stream<Arguments> filterValidationSource() {
        return Stream.<Pair<String, UnaryOperator<OrderStatusFilterBuilder>>>of(
            Pair.of(
                "Field: 'platformClientId', message: 'must not be null'",
                filter -> filter.platformClientId(null)
            ),
            Pair.of(
                "Field: 'senderId', message: 'must not be null'",
                filter -> filter.senderId(null)
            )
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Успешный поиск")
    @DatabaseSetup("/controller/order/statuses/before/search_orders_statuses.xml")
    void search(
        @SuppressWarnings("unused") String displayName,
        UnaryOperator<OrderStatusFilterBuilder> filterModifier,
        String responsePath
    ) throws Exception {
        searchOrdersStatuses(filterModifier.apply(minimalFilterBuilder()).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.<Triple<String, UnaryOperator<OrderStatusFilterBuilder>, String>>of(
            Triple.of(
                "Минимальный фильтр",
                UnaryOperator.identity(),
                "controller/order/statuses/response/1_2_4.json"
            ),
            Triple.of(
                "По идентификатору заказа",
                filter -> filter.orders(List.of(
                    OrderIdDto.builder().id(1L).build(),
                    OrderIdDto.builder().id(1L).externalId("unused-external-id").build(),
                    OrderIdDto.builder().externalId("not-found-external-id").build(),
                    OrderIdDto.builder().id(5L).build(),
                    OrderIdDto.builder().externalId("external-id-2").build()
                )),
                "controller/order/statuses/response/1_2.json"
            ),
            Triple.of(
                "Начиная с некоторого заказа",
                filter -> filter.fromOrderId(1L),
                "controller/order/statuses/response/2_4.json"
            ),
            Triple.of(
                "Количество заказов в запросе",
                filter -> filter.limit(1),
                "controller/order/statuses/response/1.json"
            ),
            Triple.of(
                "Со всеми полями",
                filter -> filter.orders(List.of(
                    OrderIdDto.builder().id(1L).build(),
                    OrderIdDto.builder().id(2L).externalId("external-id-2").build(),
                    OrderIdDto.builder().externalId("external-id-3").build()
                ))
                    .fromOrderId(1L)
                    .limit(1),
                "controller/order/statuses/response/2.json"
            ),
            Triple.of(
                "Ничего не найдено",
                filter -> filter.fromOrderId(10L),
                "controller/order/statuses/response/empty.json"
            )
        ).map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @Nonnull
    private OrderStatusFilterBuilder minimalFilterBuilder() {
        return OrderStatusFilter.builder()
            .senderId(1L)
            .platformClientId(3L);
    }

    @Nonnull
    private ResultActions searchOrdersStatuses(OrderStatusFilter filter) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/statuses/search", filter));
    }
}
