package ru.yandex.market.wms.api.service.order;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.api.service.converter.InforOrderStatusToFulfillmentOrderStatusConverter;
import ru.yandex.market.wms.api.service.order.status.OrderHistoryService;
import ru.yandex.market.wms.api.utils.OrderCheckpointDtoGenerator;
import ru.yandex.market.wms.common.model.dto.InforOrderStatusDto;
import ru.yandex.market.wms.common.model.dto.OrderCheckpointDto;
import ru.yandex.market.wms.common.model.enums.InforOrderStatusTypeToFF;
import ru.yandex.market.wms.common.model.enums.OrderCheckpoint;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.model.enums.ReturnOrderStatus;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReturnOrderStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dto.ReturnOrderStatusDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceTest {

    private static final String RECEIPT_KEY = "RCP0001";
    private static final String ORDER_KEY = "WMS0001";
    private static final String EXTERN_ORDER_KEY = "YA0001";
    private static final ResourceId ORDER_ID = new ResourceId(EXTERN_ORDER_KEY, ORDER_KEY);

    @Mock
    private OrderStatusHistoryDao orderStatusHistoryDao;
    @Mock
    private ReturnOrderStatusHistoryDao returnOrderStatusHistoryDao;
    @Mock
    private OrderDao orderDao;

    private final InforOrderStatusToFulfillmentOrderStatusConverter statusConverter =
            new InforOrderStatusToFulfillmentOrderStatusConverter();

    private OrderHistoryService orderHistoryService;

    @BeforeEach
    void beforeEach() {
        orderHistoryService = new OrderHistoryService(
                orderStatusHistoryDao,
                returnOrderStatusHistoryDao,
                statusConverter,
                orderDao
        );
    }

    @Test
    void getOrderHistoryEnrichesOrderIdWithYandexIdWhenYandexIdIsNull() {
        String testPartnerId = "testPartnerId";
        String testYandexId = "testYandexId";

        ResourceId testResourceId = new ResourceId(null, testPartnerId);

        when(orderDao.getExternOrderKeyByOrderKeys(Set.of(testPartnerId)))
                .thenReturn(Map.of(testPartnerId, testYandexId));

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(testYandexId))
                .thenReturn(historyWithParcelCreated(testPartnerId, testYandexId));
        when(returnOrderStatusHistoryDao.getReturnOrderStatuses(testYandexId)).thenReturn(List.of());

        OrderStatusHistory result = orderHistoryService.getOrderHistory(testResourceId);

        verify(orderDao, times(1)).getExternOrderKeyByOrderKeys(Set.of(testPartnerId));

        assertEquals(new ResourceId(testYandexId, testPartnerId), result.getOrderId());
    }

    @Test
    void getOrderHistoryDoesntEnrichOrderIdWithYandexIdWhenYandexIdIsNotNull() {
        String testPartnerId = "testPartnerId";
        String testYandexId = "testYandexId";

        ResourceId testResourceId = new ResourceId(testYandexId, testPartnerId);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(testYandexId))
                .thenReturn(historyWithParcelCreated(testPartnerId, testYandexId));
        when(returnOrderStatusHistoryDao.getReturnOrderStatuses(testYandexId)).thenReturn(List.of());

        OrderStatusHistory result = orderHistoryService.getOrderHistory(testResourceId);

        verify(orderDao, times(0)).findOrderByOrderKey(any());

        assertEquals(new ResourceId(testYandexId, testPartnerId), result.getOrderId());
    }

    @Test
    void getOrderHistory() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(OrderCheckpoint.CREATED, 0),
                        generator.next(OrderCheckpoint.STARTED, 1999),
                        generator.next(OrderCheckpoint.STARTED),
                        generator.next(OrderCheckpoint.PARCEL_CREATED),
                        generator.next(OrderCheckpoint.PARCEL_CREATED),
                        generator.next(OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(OrderCheckpoint.PARCEL_CREATED),
                        generator.next(OrderCheckpoint.CREATED),
                        generator.next(OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(OrderCheckpoint.PARCEL_CREATED),
                        generator.next(OrderCheckpoint.SHIPPED)
                ));
        when(returnOrderStatusHistoryDao.getReturnOrderStatuses(EXTERN_ORDER_KEY))
                .thenReturn(returnHistory(EXTERN_ORDER_KEY));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        // дата конвертируется в +03 и дробная часть секунд опускается
        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:00"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:01"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:21"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:31"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:41"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:51"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:11"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:21"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:01:31"),
                statusFf(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF, "2020-04-01T15:01:40"),
                statusFf(OrderStatusType.ORDER_SHIPPED_TO_SO_FF, "2020-04-01T15:01:41"),
                statusFf(OrderStatusType.SORTING_CENTER_RETURN_ORDER_PARTIALLY_RECEIPT_AT_SECONDARY_RECEPTION,
                        "2020-05-01T15:34:56"),
                statusFf(OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM, "2020-05-01T15:34:59")
        );
    }

    @Test
    void getOrderHistoryWhenShippedIsAfterOneSecondAfterPrev() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(OrderCheckpoint.CREATED),
                        generator.next(OrderCheckpoint.STARTED),
                        generator.next(OrderCheckpoint.PARCEL_CREATED),
                        generator.next(OrderCheckpoint.SHIPPED, 1000)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        // у 120 и 130 будет одинаковая дата
        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:20"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:30"),
                statusFf(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF, "2020-04-01T15:00:31"),
                statusFf(OrderStatusType.ORDER_SHIPPED_TO_SO_FF, "2020-04-01T15:00:31")
        );
    }

    @Test
    void getOrderHistoryWhenNoStock() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(OrderCheckpoint.CREATED),
                        generator.next(OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(OrderCheckpoint.CANCELLED)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_ITEMS_OUT_OF_STOCK_FF, "2020-04-01T15:00:20"),
                statusFf(OrderStatusType.ORDER_CANCELLED_FF, "2020-04-01T15:00:30")
        );
    }

    @Test
    void getOrderHistoryWhenShortedAndCancelled() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(OrderCheckpoint.CREATED),
                        generator.next(OrderCheckpoint.STARTED),
                        generator.next(OrderCheckpoint.PARCEL_CREATED),
                        generator.next(OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(OrderCheckpoint.CANCELLED)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:20"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:30"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:40"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:50"),
                statusFf(OrderStatusType.ORDER_ITEMS_OUT_OF_STOCK_FF, "2020-04-01T15:01:00"),
                statusFf(OrderStatusType.ORDER_CANCELLED_FF, "2020-04-01T15:01:10")
        );
    }

    private List<InforOrderStatusDto> createTestHistory(String yandexId, String partnerId) {
        return new ArrayList<>(
                List.of(
                        new InforOrderStatusDto(
                                partnerId,
                                yandexId,
                                InforOrderStatusTypeToFF.DELIVERED_ACCEPTED,
                                LocalDateTime.now(),
                                OrderType.LOAD_TESTING
                        )
                )
        );
    }

    private OrderStatus statusFf(OrderStatusType statusType, String date) {
        return new OrderStatus(statusType, new DateTime(date), null);
    }

    private List<ReturnOrderStatusDto> returnHistory(String externOrderKey) {
        return List.of(
                ReturnOrderStatusDto.builder()
                        .receiptKey(RECEIPT_KEY)
                        .externOrderKey(externOrderKey)
                        .status(ReturnOrderStatus.RECEIVING_PARTIALLY_COMPLETED)
                        .date(LocalDateTime.parse("2020-05-01T12:34:56"))
                        .build(),
                ReturnOrderStatusDto.builder()
                        .receiptKey(RECEIPT_KEY)
                        .externOrderKey(externOrderKey)
                        .status(ReturnOrderStatus.RECEIVING_COMPLETED)
                        .date(LocalDateTime.parse("2020-05-01T12:34:59"))
                        .build()
        );
    }

    private List<OrderCheckpointDto> historyWithParcelCreated(String orderKey, String externOrderKey) {
        return List.of(OrderCheckpointDto.builder()
                .orderKey(orderKey)
                .originOrderKey(orderKey)
                .externOrderKey(externOrderKey)
                .checkpoint(OrderCheckpoint.PARCEL_CREATED)
                .addDate(Instant.now())
                .build());
    }

}
