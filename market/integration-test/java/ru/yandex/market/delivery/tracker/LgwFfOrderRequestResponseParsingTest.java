package ru.yandex.market.delivery.tracker;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerEntityId;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerHistory;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerResponse;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerStatus;
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ff.LgwFfOrderClientAdapter;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType;

class LgwFfOrderRequestResponseParsingTest extends AbstractLgwParsingTest {
    private static final long SERVICE_ID = 145L;
    private static final String ORDER_HISTORY_URL = "/fulfillment/getOrderHistory";
    private static final String ORDERS_STATUS_URL = "/fulfillment/getOrdersStatus";

    private LgwFfOrderClientAdapter lgwFfOrderClientAdapter;

    @BeforeEach
    void additionalSetup() {
        FulfillmentClient fulfillmentClient = configuration.fulfillmentClient();
        lgwFfOrderClientAdapter = new LgwFfOrderClientAdapter(fulfillmentClient);
    }

    @Test
    void ffGetOrdersStatusTwoOrders() throws ParseException, IOException {
        String[] yandexIds = new String[]{"6459047", "6528157"};
        String[] partnerIds = new String[]{"EXT105716689", "EXT106217510"};

        List<TrackerHistory> expectedPayload = List.of(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                List.of(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_SHIPPED_TO_SO.getCode())
                    .date(ParsingUtils.parseDate("2019-05-10T23:53:00+03:00"))
                    .message("")
                    .build())
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                List.of(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:32:00+03:00"))
                    .message("")
                    .build())
            )
        );

        setupMockServerExpectation(ORDERS_STATUS_URL, "ff_get_orders_status_two_orders.json");

        TrackerResponse<List<TrackerHistory>> response = lgwFfOrderClientAdapter.getStatus(
            SERVICE_ID,
            List.of(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                new TrackerEntityId(yandexIds[1], partnerIds[1])
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void ffGetOrderHistoryTwoOrderStatuses() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "EXT106217510";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            List.of(
                TrackerStatus.builder()
                    .code(OrderStatusType.
                            SORTING_CENTER_RETURN_ORDER_PARTIALLY_RECEIPT_AT_SECONDARY_RECEPTION.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:19:15+03:00"))
                    .message("Возвратный заказ прошел вторичную приемку частично")
                    .build(),
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:19:15+03:00"))
                    .message("Оформлен/Готов к сбору на складе")
                    .build(),
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:19:15+03:00"))
                    .message("Новый")
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ff_get_order_history_two_statuses.json");

        TrackerResponse<TrackerHistory> response = lgwFfOrderClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void ffGetOrderHistoryDifferentTimezone() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "EXT106217510";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            List.of(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:19:15+03:00"))
                    .message("Новый")
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ff_get_order_history_timezone.json");

        TrackerResponse<TrackerHistory> response = lgwFfOrderClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void ffGetOrderHistoryWithOrderItemsAutomaticallyRemovedFf() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "EXT106217510";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            List.of(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:19:15+03:00"))
                    .message("Оформлен/Готов к сбору на складе")
                    .build(),
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:19:15+03:00"))
                    .message("Новый")
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ff_get_order_history_with_automatically_removed_item.json");

        TrackerResponse<TrackerHistory> response = lgwFfOrderClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }
}
