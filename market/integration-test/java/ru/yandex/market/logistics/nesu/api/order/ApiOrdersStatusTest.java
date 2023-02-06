package ru.yandex.market.logistics.nesu.api.order;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderIdDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusesDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusesDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderStatusFilter;
import ru.yandex.market.logistics.lom.model.filter.OrderStatusFilter.OrderStatusFilterBuilder;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.logistics.nesu.api.model.order.OrderId;
import ru.yandex.market.logistics.nesu.api.model.order.OrdersStatusFilter;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение статусов нескольких заказов")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class ApiOrdersStatusTest extends AbstractApiTest {

    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        authHolder.mockAccess(mbiApiClient, 1L);
    }

    @Test
    @DisplayName("Недоступный магазин")
    void noShopAccess() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        getStatus(defaultFilter()).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("filterValidationSource")
    @DisplayName("Валидация фильтра")
    void filterValidation(
        ValidationErrorData errorData,
        UnaryOperator<OrdersStatusFilter> filter
    ) throws Exception {
        getStatus(filter.apply(defaultFilter()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(errorData));
    }

    private static Stream<Arguments> filterValidationSource() {
        return Stream.of(
            Arguments.of(
                fieldError(
                    "senderId",
                    "must not be null",
                    "ordersStatusFilter",
                    "NotNull"
                ),
                (UnaryOperator<OrdersStatusFilter>) f -> f.setSenderId(null)
            ),
            Arguments.of(
                fieldError(
                    "orders",
                    "size must be between 1 and 100",
                    "ordersStatusFilter",
                    "Size",
                    Map.of("min", 1, "max", 100)
                ),
                (UnaryOperator<OrdersStatusFilter>) f -> f.setOrders(List.of())
            ),
            Arguments.of(
                fieldError(
                    "orders",
                    "size must be between 1 and 100",
                    "ordersStatusFilter",
                    "Size",
                    Map.of("min", 1, "max", 100)
                ),
                (UnaryOperator<OrdersStatusFilter>) f -> f.setOrders(LongStream.rangeClosed(0, 100)
                    .mapToObj(i -> new OrderId().setId(i))
                    .collect(Collectors.toList()))
            ),
            Arguments.of(
                fieldError(
                    "orders",
                    "must not contain nulls",
                    "ordersStatusFilter",
                    "NotNullElements"
                ),
                (UnaryOperator<OrdersStatusFilter>) f -> f.setOrders(Collections.singletonList(null))
            ),
            Arguments.of(
                fieldError(
                    "orders[0]",
                    "Must specify either id or externalId",
                    "ordersStatusFilter",
                    "ValidOrderId"
                ),
                (UnaryOperator<OrdersStatusFilter>) f -> f.setOrders(List.of(new OrderId()))
            )
        );
    }

    @Test
    @DisplayName("Без параметров")
    void noArgs() throws Exception {
        when(lomClient.getOrdersStatuses(defaultLomFilter().build()))
            .thenReturn(List.of(
                orderStatus(6).build(),
                orderStatus(7).build()
            ));

        getStatus(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/full.json"));
    }

    @Test
    @DisplayName("Начиная с идентификатора")
    void fromOrderId() throws Exception {
        when(lomClient.getOrdersStatuses(defaultLomFilter().fromOrderId(6L).build()))
            .thenReturn(List.of(
                orderStatus(8).build()
            ));

        getStatus(defaultFilter().setFromOrderId(6L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/single.json"));
    }

    @Test
    @DisplayName("Заказ найден по идентификатору")
    void foundById() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(List.of(OrderIdDto.builder().id(8L).build())).build()))
            .thenReturn(List.of(
                orderStatus(8).build()
            ));

        getStatus(defaultFilter().setOrders(List.of(new OrderId().setId(8L))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/single.json"));
    }

    @Test
    @DisplayName("Заказ без внешнего идентификатора")
    void noExternalId() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(List.of(OrderIdDto.builder().id(8L).build())).build()))
            .thenReturn(List.of(
                orderStatus(8).externalId(null).build()
            ));

        getStatus(defaultFilter().setOrders(List.of(new OrderId().setId(8L))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/no_external_id.json"));
    }

    @Test
    @DisplayName("Заказ найден по внешнему идентификатору")
    void foundByExternalId() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(List.of(
                OrderIdDto.builder().externalId("id-8").build()
            )).build()))
            .thenReturn(List.of(
                orderStatus(8).build()
            ));

        getStatus(defaultFilter().setOrders(List.of(new OrderId().setExternalId("id-8"))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/single.json"));
    }

    @Test
    @DisplayName("Заказ не найден по идентификатору")
    void notFoundById() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(List.of(
                OrderIdDto.builder().id(10L).build()
            )).build()))
            .thenReturn(List.of());

        getStatus(defaultFilter().setOrders(List.of(new OrderId().setId(10L))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/not_found_id.json"));
    }

    @Test
    @DisplayName("Заказ не найден по внешнему идентификатору")
    void notFoundByExternalId() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(
                List.of(OrderIdDto.builder().externalId("id-10").build())
            ).build()))
            .thenReturn(List.of());

        getStatus(defaultFilter().setOrders(List.of(new OrderId().setExternalId("id-10"))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/not_found_external_id.json"));
    }

    @Test
    @DisplayName("Порядок результатов")
    void resultOrdering() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(List.of(
                OrderIdDto.builder().id(10L).build(),
                OrderIdDto.builder().externalId("id-8").build(),
                OrderIdDto.builder().externalId("id-10").build()
            )).build()))
            .thenReturn(List.of(
                orderStatus(8).build()
            ));

        getStatus(defaultFilter().setOrders(List.of(
            new OrderId().setId(10L),
            new OrderId().setExternalId("id-8"),
            new OrderId().setExternalId("id-10")
        )))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/mixed.json"));
    }

    @Test
    @DisplayName("Статусы средней и последней мили схлопываются")
    void orderWithMiddleMile() throws Exception {
        when(lomClient
            .getOrdersStatuses(defaultLomFilter().orders(List.of(
                OrderIdDto.builder().externalId("id-8").build()
            )).build()))
            .thenReturn(List.of(
                OrderStatusesDto.builder()
                    .id(8L)
                    .externalId("id-8")
                    .orderStatus(OrderStatus.PROCESSING)
                    .globalStatusesHistory(List.of())
                    .waybillSegmentsHistories(List.of(
                        WaybillSegmentStatusesDto.builder()
                            .segmentStatus(SegmentStatus.TRACK_RECEIVED)
                            .partnerType(PartnerType.DELIVERY)
                            .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                            .segmentType(SegmentType.MOVEMENT)
                            .statusHistory(List.of(
                                WaybillSegmentStatusHistoryDto.builder()
                                    .status(SegmentStatus.STARTED)
                                    .created(Instant.parse("2019-07-03T17:00:00Z"))
                                    .build(),
                                WaybillSegmentStatusHistoryDto.builder()
                                    .status(SegmentStatus.TRACK_RECEIVED)
                                    .created(Instant.parse("2019-07-04T17:00:00Z"))
                                    .build()
                            ))
                            .build(),
                        WaybillSegmentStatusesDto.builder()
                            .segmentStatus(SegmentStatus.TRACK_RECEIVED)
                            .partnerType(PartnerType.DELIVERY)
                            .partnerSubtype(PartnerSubtype.MARKET_LOCKER)
                            .segmentType(SegmentType.PICKUP)
                            .statusHistory(List.of(
                                WaybillSegmentStatusHistoryDto.builder()
                                    .status(SegmentStatus.STARTED)
                                    .created(Instant.parse("2019-07-01T17:00:00Z"))
                                    .build(),
                                WaybillSegmentStatusHistoryDto.builder()
                                    .status(SegmentStatus.TRACK_RECEIVED)
                                    .created(Instant.parse("2019-07-02T17:00:00Z"))
                                    .build()
                            ))
                            .build()
                    ))
                    .build()
            ));

        getStatus(defaultFilter().setOrders(List.of(new OrderId().setExternalId("id-8"))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/order/status/track_received.json"));
    }

    private OrdersStatusFilter defaultFilter() {
        return new OrdersStatusFilter().setSenderId(1L);
    }

    private OrderStatusFilterBuilder defaultLomFilter() {
        return OrderStatusFilter.builder().senderId(1L).platformClientId(3L);
    }

    private OrderStatusesDto.OrderStatusesDtoBuilder orderStatus(int id) {
        return OrderStatusesDto.builder()
            .id((long) id)
            .externalId("id-" + id)
            .orderStatus(OrderStatus.PROCESSING)
            .globalStatusesHistory(List.of(
                OrderStatusHistoryDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .datetime(Instant.parse("2019-07-0" + id + "T17:00:00Z"))
                    .build()
            ))
            .waybillSegmentsHistories(List.of(
                WaybillSegmentStatusesDto.builder()
                    .segmentStatus(SegmentStatus.values()[id])
                    .partnerType(PartnerType.DELIVERY)
                    .statusHistory(List.of(
                        WaybillSegmentStatusHistoryDto.builder()
                            .status(SegmentStatus.values()[id])
                            .created(Instant.parse("2019-07-0" + id + "T17:00:00Z"))
                            .build()
                    ))
                    .build()
            ));
    }

    private ResultActions getStatus(OrdersStatusFilter defaultFilter) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/api/orders/status", defaultFilter));
    }

}
