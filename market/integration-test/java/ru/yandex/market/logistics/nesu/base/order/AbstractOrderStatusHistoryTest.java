package ru.yandex.market.logistics.nesu.base.order;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractOrderStatusHistoryTest extends AbstractContextualTest {
    protected static final Long ORDER_ID = 1L;
    public static final Set<OptionalOrderPart> OPTIONAL_PARTS = EnumSet.of(
        OptionalOrderPart.CANCELLATION_REQUESTS,
        OptionalOrderPart.CHANGE_REQUESTS,
        OptionalOrderPart.GLOBAL_STATUSES_HISTORY
    );

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        when(lomClient.getOrder(ORDER_ID, OPTIONAL_PARTS)).thenReturn(Optional.of(
            baseOrderDto()
                .setGlobalStatusesHistory(
                    List.of(
                        OrderStatusHistoryDto.builder()
                            .datetime(Instant.parse("2019-07-03T17:00:00.563426Z"))
                            .status(OrderStatus.DRAFT)
                            .build(),
                        OrderStatusHistoryDto.builder()
                            .datetime(Instant.parse("2019-07-03T17:00:00.563426Z"))
                            .status(OrderStatus.PROCESSING)
                            .build(),
                        OrderStatusHistoryDto.builder()
                            .datetime(Instant.parse("2019-07-03T17:00:00.563426Z"))
                            .status(OrderStatus.RETURNING)
                            .build()
                    )
                )
                .setWaybill(List.of(
                    WaybillSegmentDto.builder()
                        .partnerType(PartnerType.DELIVERY)
                        .waybillSegmentStatusHistory(
                            List.of(
                                WaybillSegmentStatusHistoryDto.builder()
                                    .created(Instant.parse("2019-07-04T17:00:00.563426Z"))
                                    .status(SegmentStatus.ERROR_LOST)
                                    .build(),
                                WaybillSegmentStatusHistoryDto.builder()
                                    .created(Instant.parse("2019-07-03T17:00:00.563426Z"))
                                    .status(SegmentStatus.STARTED)
                                    .build()
                            )
                        )
                        .build()
                ))
                .setCancellationOrderRequests(
                    List.of(
                        CancellationOrderRequestDto.builder()
                            .id(10L)
                            .status(CancellationOrderStatus.FAIL)
                            .created(Instant.parse("2019-07-03T18:00:00.563426Z"))
                            .updated(Instant.parse("2019-07-03T19:00:00.563426Z"))
                            .build(),
                        CancellationOrderRequestDto.builder()
                            .id(11L)
                            .status(CancellationOrderStatus.SUCCESS)
                            .created(Instant.parse("2019-07-03T20:00:00.563426Z"))
                            .updated(Instant.parse("2019-07-03T21:00:00.563426Z"))
                            .build(),
                        CancellationOrderRequestDto.builder()
                            .id(12L)
                            .status(CancellationOrderStatus.PROCESSING)
                            .created(Instant.parse("2019-07-03T22:00:00.563426Z"))
                            .updated(Instant.parse("2019-07-03T23:00:00.563426Z"))
                            .build()
                    )
                )
                .setChangeOrderRequests(
                    List.of(
                        ChangeOrderRequestDto.builder()
                            .id(4L)
                            .requestType(ChangeOrderRequestType.RECIPIENT)
                            .status(ChangeOrderRequestStatus.CREATED)
                            .created(Instant.parse("2021-06-30T15:00:00Z"))
                            .build(),
                        ChangeOrderRequestDto.builder()
                            .id(2L)
                            .requestType(ChangeOrderRequestType.DELIVERY_DATE)
                            .status(ChangeOrderRequestStatus.CREATED)
                            .created(Instant.parse("2021-06-28T00:00:00Z"))
                            .build(),
                        ChangeOrderRequestDto.builder()
                            .id(1L)
                            .requestType(ChangeOrderRequestType.RECIPIENT)
                            .status(ChangeOrderRequestStatus.SUCCESS)
                            .created(Instant.parse("2021-06-29T01:00:00Z"))
                            .updated(Instant.parse("2021-06-29T02:00:00Z"))
                            .build(),
                        ChangeOrderRequestDto.builder()
                            .id(3L)
                            .requestType(ChangeOrderRequestType.RECIPIENT)
                            .status(ChangeOrderRequestStatus.FAIL)
                            .created(Instant.parse("2021-06-30T01:00:00Z"))
                            .updated(Instant.parse("2021-06-30T02:00:00Z"))
                            .build()
                    )
                )
        ));
    }

    @AfterEach
    void tearDown() {
        verify(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/success.json"));
    }

    @Test
    @DisplayName("Заказ со средней милей в постамат. Заказ доставлен")
    void marketLockerOrderDelivered() throws Exception {
        when(lomClient.getOrder(ORDER_ID, OPTIONAL_PARTS)).thenReturn(Optional.of(
            baseOrderDto()
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
                        .partnerSubtype(PartnerSubtype.MARKET_LOCKER)
                        .waybillSegmentStatusHistory(lastMileHistoryForDelivered())
                        .build()
                ))
        ));

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/order_with_middle_mile.json"));
    }

    @Test
    @DisplayName("Заказ со средней милей в постамат Цайняо. Заказ доставлен")
    void marketGoPartnerLockerOrderDelivered() throws Exception {
        when(lomClient.getOrder(ORDER_ID, OPTIONAL_PARTS)).thenReturn(Optional.of(
            baseOrderDto()
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
                ))
        ));

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/go_partner_locker_order.json"));
    }

    @Test
    @DisplayName("Заказ со средней милей в ПВЗ. Заказ доставлен")
    void marketPartnerPickupOrderDelivered() throws Exception {
        when(lomClient.getOrder(ORDER_ID, OPTIONAL_PARTS)).thenReturn(Optional.of(
            baseOrderDto()
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
                        .partnerSubtype(PartnerSubtype.PARTNER_PICKUP_POINT_IP)
                        .waybillSegmentStatusHistory(lastMileHistoryForDelivered())
                        .build()
                ))
        ));

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/order_with_middle_mile.json"));
    }

    @Test
    @DisplayName("Заказ со средней милей в постамат. Возврат")
    void marketLockerOrderReturned() throws Exception {
        when(lomClient.getOrder(ORDER_ID, OPTIONAL_PARTS)).thenReturn(Optional.of(
            baseOrderDto()
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
                ))
        ));

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/order_with_middle_mile_locker_return.json"));
    }

    @Test
    @DisplayName("Заказ со средней милей в ПВЗ. Возврат")
    void marketPartnerPickupOrderReturned() throws Exception {
        when(lomClient.getOrder(ORDER_ID, OPTIONAL_PARTS)).thenReturn(Optional.of(
            baseOrderDto()
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
                ))
        ));

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/order_with_middle_mile_pickup_return.json"));
    }

    @Nonnull
    private List<OrderStatusHistoryDto> globalStatusesHistoryForDelivered() {
        return List.of(
            orderStatusHistoryDto(-2, OrderStatus.DRAFT),
            orderStatusHistoryDto(-1, OrderStatus.VALIDATING),
            orderStatusHistoryDto(0, OrderStatus.ENQUEUED),
            orderStatusHistoryDto(1, OrderStatus.PROCESSING),
            orderStatusHistoryDto(2, OrderStatus.PROCESSING_ERROR),
            orderStatusHistoryDto(3, OrderStatus.PROCESSING),
            orderStatusHistoryDto(6, OrderStatus.PROCESSING_ERROR),
            orderStatusHistoryDto(7, OrderStatus.PROCESSING),
            orderStatusHistoryDto(17, OrderStatus.DELIVERED)
        );
    }

    @Nonnull
    private List<OrderStatusHistoryDto> getGlobalStatusesHistoryForPickupReturned() {
        return List.of(
            orderStatusHistoryDto(-2, OrderStatus.DRAFT),
            orderStatusHistoryDto(-1, OrderStatus.VALIDATING),
            orderStatusHistoryDto(0, OrderStatus.ENQUEUED),
            orderStatusHistoryDto(1, OrderStatus.PROCESSING),
            orderStatusHistoryDto(2, OrderStatus.PROCESSING_ERROR),
            orderStatusHistoryDto(3, OrderStatus.PROCESSING),
            orderStatusHistoryDto(6, OrderStatus.PROCESSING_ERROR),
            orderStatusHistoryDto(7, OrderStatus.PROCESSING),
            orderStatusHistoryDto(18, OrderStatus.RETURNING),
            orderStatusHistoryDto(22, OrderStatus.RETURNED)
        );
    }

    @Nonnull
    private List<OrderStatusHistoryDto> globalStatusesHistoryForLockerReturned() {
        return List.of(
            orderStatusHistoryDto(-2, OrderStatus.DRAFT),
            orderStatusHistoryDto(-1, OrderStatus.VALIDATING),
            orderStatusHistoryDto(0, OrderStatus.ENQUEUED),
            orderStatusHistoryDto(1, OrderStatus.PROCESSING),
            orderStatusHistoryDto(2, OrderStatus.PROCESSING_ERROR),
            orderStatusHistoryDto(3, OrderStatus.PROCESSING),
            orderStatusHistoryDto(6, OrderStatus.PROCESSING_ERROR),
            orderStatusHistoryDto(7, OrderStatus.PROCESSING),
            orderStatusHistoryDto(18, OrderStatus.RETURNING),
            orderStatusHistoryDto(19, OrderStatus.RETURNED)
        );
    }

    @Nonnull
    private OrderStatusHistoryDto orderStatusHistoryDto(int hours, OrderStatus status) {
        return OrderStatusHistoryDto.builder()
            .datetime(getDate(hours))
            .status(status)
            .build();
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
    private WaybillSegmentStatusHistoryDto waybillSegmentStatusHistoryDto(SegmentStatus status, int hours) {
        return WaybillSegmentStatusHistoryDto.builder()
            .status(status)
            .created(getDate(hours))
            .build();
    }

    @Nonnull
    private Instant getDate(Integer hours) {
        return OffsetDateTime.parse("2020-01-01T17:00:00Z").plusHours(hours).toInstant();
    }

    @Test
    @DisplayName("Не найден заказ")
    void orderNotFound() throws Exception {
        doReturn(Optional.empty()).when(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);

        getHistory()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/statuses/not_found_order.json"));
    }

    @Test
    @DisplayName("Заказ с несуществующим в несу сендером")
    void orderSenderNotFound() throws Exception {
        doReturn(Optional.of(baseOrderDto().setSenderId(3L))).when(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);

        getHistory()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/statuses/not_found_sender_another.json"));
    }

    @Test
    @DisplayName("Пустая история заказов")
    void emptyOrderHistory() throws Exception {
        doReturn(Optional.of(baseOrderDto())).when(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/success_empty.json"));
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Получение истории статусов сегментов для заказов с маршрутом без средней мили")
    void waybillStatusHistoryForOrderWithoutMiddleMile(boolean hasLocalSc) {
        doReturn(Optional.of(
            baseOrderDto()
                .setGlobalStatusesHistory(globalStatusesHistoryForDelivered())
                .setWaybill(waybillWithoutMiddleMile(hasLocalSc)))
        )
            .when(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/order_without_middle_mile_delivered.json"));

        verify(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Получение истории статусов сегментов для заказов с маршрутом со средней милей в ПВЗ")
    void waybillStatusHistoryForOrderWithMiddleMile(boolean hasLocalSc) {
        doReturn(Optional.of(
            baseOrderDto()
                .setGlobalStatusesHistory(globalStatusesHistoryForDelivered())
                .setWaybill(waybillWithMiddleMile(hasLocalSc)))
        )
            .when(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);

        getHistory()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/statuses/order_with_middle_mile_delivered.json"));

        verify(lomClient).getOrder(ORDER_ID, OPTIONAL_PARTS);
    }

    @Nonnull
    protected abstract ResultActions getHistory() throws Exception;

    @Nonnull
    private OrderDto baseOrderDto() {
        return new OrderDto()
            .setId(ORDER_ID)
            .setExternalId("abc")
            .setPlatformClientId(3L)
            .setSenderId(1L);
    }

    @Nonnull
    private List<WaybillSegmentDto> waybillWithoutMiddleMile(boolean withLocalSc) {
        List<WaybillSegmentDto> segments = new ArrayList<>();
        segments.add(scSegment());
        if (withLocalSc) {
            segments.add(scSegment());
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
    private List<WaybillSegmentDto> waybillWithMiddleMile(boolean withLocalSc) {
        List<WaybillSegmentDto> segments = new ArrayList<>();
        segments.add(scSegment());
        if (withLocalSc) {
            segments.add(scSegment());
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
    private WaybillSegmentDto scSegment() {
        return WaybillSegmentDto.builder()
            .partnerType(PartnerType.SORTING_CENTER)
            .segmentType(SegmentType.SORTING_CENTER)
            .waybillSegmentStatusHistory(waybillSegmentStatusHistoryForSc())
            .build();
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryDto> waybillSegmentStatusHistoryForSc() {
        return List.of(
            WaybillSegmentStatusHistoryDto.builder()
                .created(Instant.parse("2020-01-01T19:00:00.563426Z"))
                .status(SegmentStatus.STARTED)
                .build(),
            WaybillSegmentStatusHistoryDto.builder()
                .created(Instant.parse("2020-01-01T19:01:00.563426Z"))
                .status(SegmentStatus.TRACK_RECEIVED)
                .build(),
            WaybillSegmentStatusHistoryDto.builder()
                .created(Instant.parse("2020-01-01T19:02:00.563426Z"))
                .status(SegmentStatus.INFO_RECEIVED)
                .trackerStatus("SORTING_CENTER_LOADED")
                .build(),
            WaybillSegmentStatusHistoryDto.builder()
                .created(Instant.parse("2020-01-01T19:03:00.563426Z"))
                .status(SegmentStatus.IN)
                .trackerStatus("SORTING_CENTER_AT_START")
                .build(),
            WaybillSegmentStatusHistoryDto.builder()
                .created(Instant.parse("2020-01-01T19:04:00.563426Z"))
                .status(SegmentStatus.TRANSIT_PREPARED)
                .trackerStatus("SORTING_CENTER_PREPARED")
                .build(),
            WaybillSegmentStatusHistoryDto.builder()
                .created(Instant.parse("2020-01-01T19:05:00.563426Z"))
                .status(SegmentStatus.OUT)
                .trackerStatus("SORTING_CENTER_TRANSMITTED")
                .build()
        );
    }
}
