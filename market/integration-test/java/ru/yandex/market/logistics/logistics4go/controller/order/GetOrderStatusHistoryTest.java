package ru.yandex.market.logistics.logistics4go.controller.order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.logistics.logistics4go.client.model.GetOrderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.OrderStatus;
import ru.yandex.market.logistics.logistics4go.client.model.OrderStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4go.utils.LomFactory.order;

@DisplayName("Конвертация истории статусов заказа в ручке получения заказа")
class GetOrderStatusHistoryTest extends AbstractOrderTest {
    private static final long ORDER_ID = 1;

    @ParameterizedTest
    @ValueSource(strings = {"MARKET_LOCKER", "PARTNER_PICKUP_POINT_IP"})
    @DisplayName("Заказ со средней милей в постамат. Заказ доставлен")
    void marketLockerOrderDelivered(PartnerSubtype partnerSubtype) throws Exception {
        OrderDto lomOrder = baseOrderDto()
            .setGlobalStatusesHistory(globalStatusesHistoryForDelivered())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                    .waybillSegmentStatusHistory(middleMileHistoryForDelivered())
                    .build(),
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.PICKUP)
                    .partnerSubtype(partnerSubtype)
                    .waybillSegmentStatusHistory(lastMileHistoryForDelivered())
                    .build()
            ));

        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(OrderStatus.DELIVERY_DELIVERED);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of(
                l4gOrderStatusHistoryDto(OrderStatus.DRAFT, "2020-01-01T15:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.VALIDATING, "2020-01-01T16:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.CREATED, "2020-01-01T17:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T18:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T19:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T20:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T23:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-02T00:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRACK_RECEIVED, "2020-01-02T01:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SENDER_SENT, "2020-01-02T02:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_LOADED, "2020-01-02T03:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START, "2020-01-02T04:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START_SORT, "2020-01-02T05:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRANSPORTATION, "2020-01-02T06:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T09:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_DELIVERED, "2020-01-02T10:00:00Z")
            ));
        }
    }

    @Test
    @DisplayName("Заказ со средней милей в постамат Цайняо. Заказ доставлен")
    void marketGoPartnerLockerOrderDelivered() throws Exception {
        OrderDto lomOrder = baseOrderDto()
            .setGlobalStatusesHistory(globalStatusesHistoryForDelivered())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                    .waybillSegmentStatusHistory(middleMileHistoryForGoPartnerLocker())
                    .build(),
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.PICKUP)
                    .partnerSubtype(PartnerSubtype.GO_PARTNER_LOCKER)
                    .waybillSegmentStatusHistory(lastMileHistoryForDelivered())
                    .build()
            ));

        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(OrderStatus.DELIVERY_DELIVERED);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of(
                l4gOrderStatusHistoryDto(OrderStatus.DRAFT, "2020-01-01T15:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.VALIDATING, "2020-01-01T16:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.CREATED, "2020-01-01T17:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T18:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T19:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T20:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRACK_RECEIVED, "2020-01-01T23:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SENDER_SENT, "2020-01-02T00:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_LOADED, "2020-01-02T01:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START, "2020-01-02T02:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START_SORT, "2020-01-02T03:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRANSPORTATION, "2020-01-02T04:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T05:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T09:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_DELIVERED, "2020-01-02T10:00:00Z")
            ));
        }
    }

    @Test
    @DisplayName("Заказ со средней милей в постамат. Возврат")
    void marketLockerOrderReturned() throws Exception {
        OrderDto lomOrder = baseOrderDto()
            .setGlobalStatusesHistory(globalStatusesHistoryForLockerReturned())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                    .waybillSegmentStatusHistory(middleMileHistoryForLockerReturned())
                    .build(),
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.PICKUP)
                    .partnerSubtype(PartnerSubtype.PARTNER_PICKUP_POINT_IP)
                    .waybillSegmentStatusHistory(lastMileHistoryForLockerReturned())
                    .build()
            ));
        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(OrderStatus.RETURN_TRANSMITTED_FULFILMENT);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of(
                l4gOrderStatusHistoryDto(OrderStatus.DRAFT, "2020-01-01T15:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.VALIDATING, "2020-01-01T16:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.CREATED, "2020-01-01T17:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T18:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T19:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T20:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T23:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-02T00:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRACK_RECEIVED, "2020-01-02T01:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SENDER_SENT, "2020-01-02T02:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_LOADED, "2020-01-02T03:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START, "2020-01-02T04:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START_SORT, "2020-01-02T05:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRANSPORTATION, "2020-01-02T06:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T09:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_STORAGE_PERIOD_EXPIRED, "2020-01-02T10:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.RETURN_ARRIVED_DELIVERY, "2020-01-02T11:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.RETURN_TRANSMITTED_FULFILMENT, "2020-01-02T12:00:00Z")
            ));
        }
    }

    @Test
    @DisplayName("Заказ со средней милей в ПВЗ. Возврат")
    void marketPartnerPickupOrderReturned() throws Exception {
        OrderDto lomOrder = baseOrderDto()
            .setGlobalStatusesHistory(getGlobalStatusesHistoryForPickupReturned())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                    .waybillSegmentStatusHistory(middleMileHistoryForPickupReturned())
                    .build(),
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.PICKUP)
                    .partnerSubtype(PartnerSubtype.PARTNER_PICKUP_POINT_IP)
                    .waybillSegmentStatusHistory(lastMileHistoryForPickupReturned())
                    .build()
            ));

        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(OrderStatus.RETURN_TRANSMITTED_FULFILMENT);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of(
                l4gOrderStatusHistoryDto(OrderStatus.DRAFT, "2020-01-01T15:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.VALIDATING, "2020-01-01T16:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.CREATED, "2020-01-01T17:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T18:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T19:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T20:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T23:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-02T00:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRACK_RECEIVED, "2020-01-02T01:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SENDER_SENT, "2020-01-02T02:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_LOADED, "2020-01-02T03:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START, "2020-01-02T04:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START_SORT, "2020-01-02T05:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRANSPORTATION, "2020-01-02T06:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T09:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_STORAGE_PERIOD_EXPIRED, "2020-01-02T10:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.RETURN_ARRIVED_DELIVERY, "2020-01-02T14:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.RETURN_TRANSMITTED_FULFILMENT, "2020-01-02T15:00:00Z")
            ));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Получение истории статусов сегментов для заказов с маршрутом без средней мили")
    void waybillStatusHistoryForOrderWithoutMiddleMile(boolean hasLocalSortingCenter) throws Exception {
        OrderDto lomOrder = baseOrderDto()
            .setGlobalStatusesHistory(globalStatusesHistoryForDelivered())
            .setWaybill(waybillWithoutMiddleMile(hasLocalSortingCenter));
        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(OrderStatus.DELIVERY_DELIVERED);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of(
                l4gOrderStatusHistoryDto(OrderStatus.DRAFT, "2020-01-01T15:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.VALIDATING, "2020-01-01T16:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.CREATED, "2020-01-01T17:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T18:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T19:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_PROCESSING_STARTED, "2020-01-01T19:01:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_TRACK_RECEIVED, "2020-01-01T19:02:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_LOADED, "2020-01-01T19:03:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_AT_START, "2020-01-01T19:04:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_PREPARED, "2020-01-01T19:05:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_TRANSMITTED, "2020-01-01T19:06:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T20:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRACK_RECEIVED, "2020-01-01T21:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SENDER_SENT, "2020-01-01T22:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_LOADED, "2020-01-01T23:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START, "2020-01-02T07:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRANSPORTATION, "2020-01-02T08:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T09:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_DELIVERED, "2020-01-02T10:00:00Z")
            ));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Получение истории статусов сегментов для заказов с маршрутом со средней милей в ПВЗ")
    void waybillStatusHistoryForOrderWithMiddleMile(boolean hasLocalSc) throws Exception {
        OrderDto lomOrder = baseOrderDto()
            .setGlobalStatusesHistory(globalStatusesHistoryForDelivered())
            .setWaybill(waybillWithMiddleMile(hasLocalSc));

        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(OrderStatus.DELIVERY_DELIVERED);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of(
                l4gOrderStatusHistoryDto(OrderStatus.DRAFT, "2020-01-01T15:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.VALIDATING, "2020-01-01T16:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.CREATED, "2020-01-01T17:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T18:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T19:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_PROCESSING_STARTED, "2020-01-01T19:01:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_TRACK_RECEIVED, "2020-01-01T19:02:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_LOADED, "2020-01-01T19:03:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_AT_START, "2020-01-01T19:04:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_PREPARED, "2020-01-01T19:05:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SORTING_CENTER_TRANSMITTED, "2020-01-01T19:06:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T20:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-01T22:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.ERROR, "2020-01-01T23:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_PROCESSING_STARTED, "2020-01-02T00:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRACK_RECEIVED, "2020-01-02T01:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.SENDER_SENT, "2020-01-02T02:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_LOADED, "2020-01-02T03:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START, "2020-01-02T04:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_AT_START_SORT, "2020-01-02T05:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_TRANSPORTATION, "2020-01-02T06:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_ARRIVED_PICKUP_POINT, "2020-01-02T09:00:00Z"),
                l4gOrderStatusHistoryDto(OrderStatus.DELIVERY_DELIVERED, "2020-01-02T10:00:00Z")
            ));
        }
    }

    @Test
    @DisplayName("Пустая история статусов")
    void emptyOrderHistory() throws Exception {
        OrderDto lomOrder = baseOrderDto().setGlobalStatusesHistory(List.of());
        lomOrder.setWaybill(
            lomOrder.getWaybill().stream()
                .map(segment -> segment.toBuilder().waybillSegmentStatusHistory(List.of()).build())
                .collect(Collectors.toList())
        );

        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse l4gOrder = getOrder();
            softly.assertThat(l4gOrder.getStatus()).isEqualTo(null);
            softly.assertThat(l4gOrder.getStatusHistory()).isEqualTo(List.of());
        }
    }

    @Nonnull
    private OrderDto baseOrderDto() {
        return order(false);
    }

    @Nonnull
    protected GetOrderResponse getOrder() {
        return apiClient.orders()
            .getOrder()
            .orderIdPath(ORDER_ID)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Nonnull
    private AutoCloseable mockLomClientSearchOrders(@Nullable OrderDto response) {
        OrderSearchFilter orderFilter = getOrderFilter(ORDER_ID);
        List<OrderDto> orders = Stream.ofNullable(response).filter(Objects::nonNull).collect(Collectors.toList());
        doReturn(PageResult.of(orders, orders.size(), 1, 1))
            .when(lomClient)
            .searchOrders(
                orderFilter,
                OPTIONAL_ORDER_PARTS,
                Pageable.unpaged()
            );
        return () -> verify(lomClient).searchOrders(orderFilter, OPTIONAL_ORDER_PARTS, Pageable.unpaged());
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> middleMileHistoryForDelivered() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.ERROR, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 7),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 8),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 9),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 10),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 11),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT, 12),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 13)
        );
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> middleMileHistoryForGoPartnerLocker() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 7),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 8),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 9),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT, 10),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 11),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_PICKUP, 12)
        );
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> lastMileHistoryForDelivered() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 1),
            waybillSegmentStatusHistoryDto(SegmentStatus.ERROR, 2),
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 3),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 4),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 14),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 15),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_PICKUP, 16),
            waybillSegmentStatusHistoryDto(SegmentStatus.OUT, 17)
        );
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> lastMileHistoryForPickupReturned() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 1),
            waybillSegmentStatusHistoryDto(SegmentStatus.ERROR, 2),
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 3),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 4),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 14),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 15),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_PICKUP, 16),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED, 17),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURN_PREPARING, 18),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURN_ARRIVED, 19),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURNED, 20)
        );
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> middleMileHistoryForPickupReturned() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.ERROR, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 7),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 8),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 9),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 10),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 11),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT, 12),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 13),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURN_ARRIVED, 21),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURNED, 22)
        );
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> middleMileHistoryForLockerReturned() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.ERROR, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 7),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 8),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 9),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 10),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 11),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT, 12),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 13),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURN_ARRIVED, 18),
            waybillSegmentStatusHistoryDto(SegmentStatus.RETURNED, 19)
        );
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> lastMileHistoryForLockerReturned() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 1),
            waybillSegmentStatusHistoryDto(SegmentStatus.ERROR, 2),
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 3),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 4),
            waybillSegmentStatusHistoryDto(SegmentStatus.PENDING, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 6),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 14),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, 15),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_PICKUP, 16),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED, 17)
        );
    }

    @Nonnull
    private List<ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto> globalStatusesHistoryForDelivered() {
        return List.of(
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DRAFT, -2),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.VALIDATING, -1),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.ENQUEUED, 0),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 1),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING_ERROR, 2),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 3),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING_ERROR, 6),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 7),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DELIVERED, 17)
        );
    }

    @Nonnull
    private List<ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto>
    getGlobalStatusesHistoryForPickupReturned() {
        return List.of(
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DRAFT, -2),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.VALIDATING, -1),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.ENQUEUED, 0),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 1),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING_ERROR, 2),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 3),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING_ERROR, 6),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 7),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.RETURNING, 18),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.RETURNED, 22)
        );
    }

    @Nonnull
    private List<ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto>
    globalStatusesHistoryForLockerReturned() {
        return List.of(
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DRAFT, -2),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.VALIDATING, -1),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.ENQUEUED, 0),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 1),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING_ERROR, 2),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 3),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING_ERROR, 6),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING, 7),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.RETURNING, 18),
            lomOrderStatusHistoryDto(ru.yandex.market.logistics.lom.model.enums.OrderStatus.RETURNED, 19)
        );
    }

    @Nonnull
    private ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto lomOrderStatusHistoryDto(
        ru.yandex.market.logistics.lom.model.enums.OrderStatus status,
        int hours
    ) {
        return ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto.builder()
            .datetime(instant(hours))
            .status(status)
            .build();
    }

    @Nonnull
    private OrderStatusHistoryDto l4gOrderStatusHistoryDto(OrderStatus status, String isoInstantText) {
        return new OrderStatusHistoryDto().datetime(Instant.parse(isoInstantText)).status(status);
    }

    @Nonnull
    private WaybillSegmentStatusHistoryDto waybillSegmentStatusHistoryDto(SegmentStatus status, int hours) {
        return waybillSegmentStatusHistoryDto(status, hours, 0);
    }

    @Nonnull
    private WaybillSegmentStatusHistoryDto waybillSegmentStatusHistoryDto(
        SegmentStatus status,
        int hours,
        int minutes
    ) {
        return WaybillSegmentStatusHistoryDto.builder()
            .status(status)
            .created(instant(hours, minutes))
            .build();
    }

    @Nonnull
    private Instant instant(int hours) {
        return instant(hours, 0);
    }

    @Nonnull
    private Instant instant(int hours, int minutes) {
        return Instant.parse("2020-01-01T17:00:00Z")
            .plus(hours, ChronoUnit.HOURS)
            .plus(minutes, ChronoUnit.MINUTES);
    }

    @Nonnull
    private List<WaybillSegmentDto> waybillWithMiddleMile(boolean withLocalSortingCenter) {
        List<WaybillSegmentDto> segments = new ArrayList<>();
        segments.add(sortingCenterSegment());
        if (withLocalSortingCenter) {
            segments.add(sortingCenterSegment());
        }
        segments.add(
            WaybillSegmentDto.builder()
                .partnerType(PartnerType.DELIVERY)
                .segmentType(SegmentType.PICKUP)
                .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                .waybillSegmentStatusHistory(middleMileHistoryForDelivered())
                .build()
        );
        segments.add(
            WaybillSegmentDto.builder()
                .partnerType(PartnerType.DELIVERY)
                .segmentType(SegmentType.PICKUP)
                .partnerSubtype(PartnerSubtype.PARTNER_PICKUP_POINT_IP)
                .waybillSegmentStatusHistory(lastMileHistoryForDelivered())
                .build()
        );

        return segments;
    }

    @Nonnull
    private List<WaybillSegmentDto> waybillWithoutMiddleMile(boolean withLocalSortingCenter) {
        List<WaybillSegmentDto> segments = new ArrayList<>();
        segments.add(sortingCenterSegment());
        if (withLocalSortingCenter) {
            segments.add(sortingCenterSegment());
        }
        segments.add(
            WaybillSegmentDto.builder()
                .partnerType(PartnerType.DELIVERY)
                .segmentType(SegmentType.COURIER)
                .partnerSubtype(PartnerSubtype.MARKET_COURIER)
                .waybillSegmentStatusHistory(lastMileHistoryForDelivered())
                .build()
        );

        return segments;
    }

    @Nonnull
    private WaybillSegmentDto sortingCenterSegment() {
        return WaybillSegmentDto.builder()
            .partnerType(PartnerType.SORTING_CENTER)
            .segmentType(SegmentType.SORTING_CENTER)
            .waybillSegmentStatusHistory(waybillSegmentStatusHistoryForSortingCenter())
            .build();
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> waybillSegmentStatusHistoryForSortingCenter() {
        return List.of(
            waybillSegmentStatusHistoryDto(SegmentStatus.STARTED, 2, 1),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRACK_RECEIVED, 2, 2),
            waybillSegmentStatusHistoryDto(SegmentStatus.INFO_RECEIVED, 2, 3),
            waybillSegmentStatusHistoryDto(SegmentStatus.IN, 2, 4),
            waybillSegmentStatusHistoryDto(SegmentStatus.TRANSIT_PREPARED, 2, 5),
            waybillSegmentStatusHistoryDto(SegmentStatus.OUT, 2, 6)
        );
    }
}
