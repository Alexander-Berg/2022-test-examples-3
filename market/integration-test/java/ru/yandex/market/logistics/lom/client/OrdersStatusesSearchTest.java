package ru.yandex.market.logistics.lom.client;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.model.dto.OrderIdDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusesDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusesDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderStatusFilter;

@ParametersAreNonnullByDefault
class OrdersStatusesSearchTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск истории статусов заказов")
    void searchOrders() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/orders/statuses/search",
            "request/order/statuses/search_by_all_fields.json",
            "response/order/statuses/history_search.json"
        );

        OrderStatusFilter filter = OrderStatusFilter.builder()
            .platformClientId(3L)
            .senderId(1L)
            .orders(List.of(
                OrderIdDto.builder().id(1L).externalId("external-id-3").build(),
                OrderIdDto.builder().id(2L).build(),
                OrderIdDto.builder().id(3L).build()
            ))
            .fromOrderId(0L)
            .limit(2)
            .build();

        List<OrderStatusesDto> actual = lomClient.getOrdersStatuses(filter);

        List<OrderStatusesDto> expected = List.of(
            OrderStatusesDto.builder()
                .id(1L)
                .externalId("external-id-1")
                .orderStatus(OrderStatus.DRAFT)
                .globalStatusesHistory(List.of(
                    getOrderStatusHistoryDto(1L, "2018-02-02T12:00:00Z", OrderStatus.DRAFT),
                    getOrderStatusHistoryDto(2L, "2018-02-03T12:00:00Z", OrderStatus.VALIDATION_ERROR),
                    getOrderStatusHistoryDto(3L, "2018-02-04T12:00:00Z", OrderStatus.DRAFT)
                ))
                .waybillSegmentsHistories(List.of())
                .build(),
            OrderStatusesDto.builder()
                .id(2L)
                .externalId("external-id-2")
                .orderStatus(OrderStatus.RETURNING)
                .globalStatusesHistory(List.of(
                    getOrderStatusHistoryDto(4L, "2018-03-02T12:00:00Z", OrderStatus.DRAFT),
                    getOrderStatusHistoryDto(5L, "2018-03-03T12:00:00Z", OrderStatus.PROCESSING),
                    getOrderStatusHistoryDto(6L, "2018-03-05T12:00:00Z", OrderStatus.RETURNING)
                ))
                .waybillSegmentsHistories(List.of(
                    WaybillSegmentStatusesDto.builder()
                        .partnerType(PartnerType.SORTING_CENTER)
                        .segmentType(SegmentType.SORTING_CENTER)
                        .segmentStatus(SegmentStatus.OUT)
                        .statusHistory(List.of(
                            getWaybillSegmentStatusHistoryDto(
                                3L,
                                SegmentStatus.PENDING,
                                "2018-03-03T11:00:00Z",
                                "2018-03-03T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                4L,
                                SegmentStatus.INFO_RECEIVED,
                                "2018-03-03T11:30:00Z",
                                "2018-03-03T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                5L,
                                SegmentStatus.IN,
                                "2018-03-04T11:00:00Z",
                                "2018-03-04T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                6L,
                                SegmentStatus.OUT,
                                "2018-03-04T11:45:00Z",
                                "2018-03-04T12:00:00Z"
                            )
                        ))
                        .build(),
                    WaybillSegmentStatusesDto.builder()
                        .partnerType(PartnerType.DELIVERY)
                        .segmentType(SegmentType.MOVEMENT)
                        .segmentStatus(SegmentStatus.RETURN_PREPARING)
                        .statusHistory(List.of(
                            getWaybillSegmentStatusHistoryDto(
                                7L,
                                SegmentStatus.PENDING,
                                "2018-03-03T11:00:00Z",
                                "2018-03-03T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                8L,
                                SegmentStatus.INFO_RECEIVED,
                                "2018-03-03T11:05:00Z",
                                "2018-03-03T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                9L,
                                SegmentStatus.IN,
                                "2018-03-04T11:50:00Z",
                                "2018-03-04T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                10L,
                                SegmentStatus.RETURN_PREPARING,
                                "2018-03-05T11:00:00Z",
                                "2018-03-05T12:00:00Z"
                            )
                        ))
                        .build(),
                    WaybillSegmentStatusesDto.builder()
                        .partnerType(PartnerType.DELIVERY)
                        .segmentType(SegmentType.PICKUP)
                        .segmentStatus(SegmentStatus.INFO_RECEIVED)
                        .statusHistory(List.of(
                            getWaybillSegmentStatusHistoryDto(
                                11L,
                                SegmentStatus.PENDING,
                                "2018-03-03T11:00:00Z",
                                "2018-03-03T12:00:00Z"
                            ),
                            getWaybillSegmentStatusHistoryDto(
                                12L,
                                SegmentStatus.INFO_RECEIVED,
                                "2018-03-03T11:05:00Z",
                                "2018-03-03T12:00:00Z"
                            )
                        ))
                        .build()
                ))
                .build()
        );

        softly.assertThat(actual).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Nonnull
    private WaybillSegmentStatusHistoryDto getWaybillSegmentStatusHistoryDto(
        long id,
        SegmentStatus status,
        String date,
        String created
    ) {
        return WaybillSegmentStatusHistoryDto.builder()
            .id(id)
            .status(status)
            .date(Instant.parse(date))
            .created(Instant.parse(created))
            .build();
    }

    @Nonnull
    private OrderStatusHistoryDto getOrderStatusHistoryDto(long id, String datetime, OrderStatus status) {
        return OrderStatusHistoryDto.builder()
            .id(id)
            .datetime(Instant.parse(datetime))
            .status(status)
            .build();
    }
}
