package ru.yandex.market.wms.api.service.order;

import java.util.List;

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
import ru.yandex.market.wms.common.model.enums.OrderCheckpoint;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReturnOrderStatusHistoryDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceForSplitOrdersTest {

    private static final String ORIGIN_ORDER_KEY = "WMS0001";
    private static final String EXTERN_ORDER_KEY = "YA0001";
    private static final ResourceId ORDER_ID = new ResourceId(EXTERN_ORDER_KEY, ORIGIN_ORDER_KEY);

    private static final String CHILD_ORDER1 = "WMS00011";
    private static final String CHILD_ORDER2 = "WMS00012";
    private static final String CHILD_ORDER3 = "WMS00013";

    @Mock
    private DbConfigService configService;
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
    void getOrderHistoryWhenAllShipped() {
        // original order split into 3 parts
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(CHILD_ORDER1, OrderCheckpoint.CREATED, 0),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CREATED, 10),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.CREATED, 20),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.SHIPPED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.SHIPPED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.SHIPPED)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:00"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:30"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:40"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:50"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:01:10"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:40"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:01:50"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:02:10"),
                statusFf(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF, "2020-04-01T15:02:19"),
                statusFf(OrderStatusType.ORDER_SHIPPED_TO_SO_FF, "2020-04-01T15:02:20")
        );
    }

    @Test
    void getOrderHistoryWhenAllCancelled() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(CHILD_ORDER1, OrderCheckpoint.CREATED, 0),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CREATED, 10),

                        generator.next(CHILD_ORDER1, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.CANCELLED),

                        generator.next(CHILD_ORDER2, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CANCELLED)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:00"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:20"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:30"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:00"),
                statusFf(OrderStatusType.ORDER_ITEMS_OUT_OF_STOCK_FF, "2020-04-01T15:01:10"),
                statusFf(OrderStatusType.ORDER_CANCELLED_FF, "2020-04-01T15:01:20")
        );
    }

    @Test
    void getOrderHistoryWhenSomeShippedSomeCancelledLastShipped() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(CHILD_ORDER1, OrderCheckpoint.CREATED, 0),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CREATED, 10),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.CREATED, 20),

                        generator.next(CHILD_ORDER1, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.SHIPPED),

                        generator.next(CHILD_ORDER2, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CANCELLED),

                        generator.next(CHILD_ORDER3, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.SHIPPED)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:00"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:20"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:50"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:00"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:01:30"),
                statusFf(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF, "2020-04-01T15:01:39"),
                statusFf(OrderStatusType.ORDER_SHIPPED_TO_SO_FF, "2020-04-01T15:01:40")
        );
    }

    @Test
    void getOrderHistoryWhenSomeShippedSomeCancelledLastCancelled() {
        var generator = new OrderCheckpointDtoGenerator(ORDER_ID);

        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(EXTERN_ORDER_KEY))
                .thenReturn(List.of(
                        generator.next(CHILD_ORDER1, OrderCheckpoint.CREATED, 0),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CREATED, 10),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.CREATED, 20),

                        generator.next(CHILD_ORDER1, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.PARCEL_CREATED),
                        generator.next(CHILD_ORDER1, OrderCheckpoint.SHIPPED),

                        generator.next(CHILD_ORDER2, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(CHILD_ORDER2, OrderCheckpoint.CANCELLED),

                        generator.next(CHILD_ORDER3, OrderCheckpoint.STARTED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.ITEMS_SHORTED),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.ITEMS_OUT_OF_STOCK),
                        generator.next(CHILD_ORDER3, OrderCheckpoint.CANCELLED)
                ));

        OrderStatusHistory history = orderHistoryService.getOrderHistory(ORDER_ID);

        assertThat(history.getHistory()).containsExactly(
                statusFf(OrderStatusType.ORDER_CREATED_FF, "2020-04-01T15:00:00"),
                statusFf(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, "2020-04-01T15:00:10"),
                statusFf(OrderStatusType.ORDER_PLACES_CHANGED_FF, "2020-04-01T15:00:20"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:00:50"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:20"),
                statusFf(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF, "2020-04-01T15:01:30"),
                statusFf(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF, "2020-04-01T15:01:39"),
                statusFf(OrderStatusType.ORDER_SHIPPED_TO_SO_FF, "2020-04-01T15:01:40")
        );
    }

    private OrderStatus statusFf(OrderStatusType statusType, String date) {
        return new OrderStatus(statusType, new DateTime(date), null);

    }

}
