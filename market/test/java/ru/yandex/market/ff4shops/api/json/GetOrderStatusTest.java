package ru.yandex.market.ff4shops.api.json;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.OrderHistoryUtil;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static ru.yandex.market.ff4shops.api.OrderHistoryUtil.eventSubstatusUpdated;
import static ru.yandex.market.ff4shops.api.OrderHistoryUtil.itemNotFoundEvent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DbUnitDataSet(before = "Environment.useYandexIdInsteadExternalId.before.csv")
public class GetOrderStatusTest extends AbstractJsonControllerFunctionalTest {

    @Autowired
    private CheckouterAPI checkouterAPI;

    private CheckouterOrderHistoryEventsApi historyEventsApi;

    @BeforeEach
    void setup() {
        historyEventsApi = OrderHistoryUtil.prepareMock(checkouterAPI);
    }

    @Test
    void singleOrderMultipleEvents() {
        mockHistoryEvents();
        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_single.json",
            "ru/yandex/market/ff4shops/api/json/get_order_status_single_response.json"
        );
    }

    @Test
    void emptyOrderIds() {
        mockHistoryEvents();
        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_empty.json",
            "ru/yandex/market/ff4shops/api/json/get_order_status_empty_response.json"
        );
    }

    private void mockHistoryEvents() {
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(
                eventSubstatusUpdated(1,
                    OrderHistoryUtil.YANDEX_ID, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.STARTED
                ),
                eventSubstatusUpdated(2,
                    OrderHistoryUtil.YANDEX_ID, LocalDateTime.of(2020, 2, 3, 4, 5, 6), OrderSubstatus.READY_TO_SHIP
                )
            ),
            OrderHistoryUtil.YANDEX_ID
        );
    }

    @Test
    void multipleOrders() {
        long first = 2;
        long second = 3;
        long third = 4;
        long fourth = 5;
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(
                eventSubstatusUpdated(1, first, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.STARTED),
                eventSubstatusUpdated(2, first, LocalDateTime.of(2020, 2, 3, 4, 5, 6), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(3, second, LocalDateTime.of(2020, 3, 4, 5, 6, 7), OrderSubstatus.STARTED),
                eventSubstatusUpdated(4, third, LocalDateTime.of(2020, 4, 5, 6, 7, 8), OrderSubstatus.PACKAGING),
                eventSubstatusUpdated(5, fourth, LocalDateTime.of(2020, 5, 6, 7, 8, 9), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(6, fourth, LocalDateTime.of(2020, 5, 6, 7, 8, 9), OrderSubstatus.SHIPPED)
            ),
            first,
            second,
            third,
            fourth
        );

        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_multiple.json",
            "ru/yandex/market/ff4shops/api/json/get_order_status_multiple_response.json"
        );
    }

    @Test
    @DisplayName("Cабстатус SHIPPED не приходит,  130 чп выключен")
    void multipleOrdersWithoutShippedSubstatus() {
        long first = 2;
        long second = 3;
        long third = 4;
        long fourth = 5;
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(
                eventSubstatusUpdated(1, first, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.STARTED),
                eventSubstatusUpdated(2, first, LocalDateTime.of(2020, 2, 3, 4, 5, 6), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(3, second, LocalDateTime.of(2020, 3, 4, 5, 6, 7), OrderSubstatus.STARTED),
                eventSubstatusUpdated(4, third, LocalDateTime.of(2020, 4, 5, 6, 7, 8), OrderSubstatus.PACKAGING)
            ),
            first,
            second,
            third,
            fourth
        );

        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_multiple.json",
            "ru/yandex/market/ff4shops/api/json/get_order_status_multiple_without_shipped.json"
        );
    }

    @Test
    @DisplayName("Статус удаления товаров из заказа")
    void removedItems() {
        long first = 2;
        long second = 3;
        long third = 4;
        long fourth = 5;
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(
                eventSubstatusUpdated(1, first, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.STARTED),
                itemNotFoundEvent(2, first, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.PACKAGING),
                eventSubstatusUpdated(3, first, LocalDateTime.of(2020, 2, 3, 4, 5, 6), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(4, second, LocalDateTime.of(2020, 3, 4, 5, 6, 7), OrderSubstatus.STARTED),
                eventSubstatusUpdated(5, third, LocalDateTime.of(2020, 4, 5, 6, 7, 8), OrderSubstatus.PACKAGING),
                itemNotFoundEvent(6, third, LocalDateTime.of(2020, 4, 5, 6, 7, 9), OrderSubstatus.PACKAGING),
                eventSubstatusUpdated(7, fourth, LocalDateTime.of(2020, 5, 6, 7, 8, 9), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(8, fourth, LocalDateTime.of(2020, 5, 6, 7, 8, 9), OrderSubstatus.SHIPPED)
            ),
            first,
            second,
            third,
            fourth
        );

        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_multiple.json",
            "ru/yandex/market/ff4shops/api/json/get_order_status_with_114.json"
        );
    }

    @Test
    @DisplayName("Приходит сабстатус SHIPPED, но не обрабатывается, 130 чекпоинт выключен")
    void removedItemsWithShippedNotProcessed() {
        long first = 2;
        long second = 3;
        long third = 4;
        long fourth = 5;
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(
                eventSubstatusUpdated(1, first, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.STARTED),
                itemNotFoundEvent(2, first, LocalDateTime.of(2020, 1, 2, 3, 4, 5), OrderSubstatus.PACKAGING),
                eventSubstatusUpdated(3, first, LocalDateTime.of(2020, 2, 3, 4, 5, 6), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(4, second, LocalDateTime.of(2020, 3, 4, 5, 6, 7), OrderSubstatus.STARTED),
                eventSubstatusUpdated(5, third, LocalDateTime.of(2020, 4, 5, 6, 7, 8), OrderSubstatus.PACKAGING),
                itemNotFoundEvent(6, third, LocalDateTime.of(2020, 4, 5, 6, 7, 9), OrderSubstatus.PACKAGING),
                eventSubstatusUpdated(7, fourth, LocalDateTime.of(2020, 5, 6, 7, 8, 9), OrderSubstatus.READY_TO_SHIP),
                eventSubstatusUpdated(8, fourth, LocalDateTime.of(2020, 5, 6, 7, 8, 20), OrderSubstatus.SHIPPED)
            ),
            first,
            second,
            third,
            fourth
        );

        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_multiple.json",
            "ru/yandex/market/ff4shops/api/json/get_order_status_ignore_shipped_response.json"
        );
    }

    @Test
    void unknownStatus() {
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(eventSubstatusUpdated(
                1,
                OrderHistoryUtil.YANDEX_ID,
                LocalDateTime.of(2020, 1, 2, 3, 4, 5),
                OrderSubstatus.PENDING_CANCELLED
            )),
            OrderHistoryUtil.YANDEX_ID
        );

        assertGetOrdersStatus(
            "ru/yandex/market/ff4shops/api/json/get_order_status_single.json",
            "ru/yandex/market/ff4shops/api/json/get_orders_status_unknown.json"
        );
    }

    private void assertGetOrdersStatus(String requestPath, String responsePath) {
        String response = FunctionalTestHelper
            .postForEntity(
                FF4ShopsUrlBuilder.getOrdersStatus(randomServerPort),
                extractFileContent(requestPath),
                FunctionalTestHelper.jsonHeaders()
            )
            .getBody();
        assertResponseBody(response, responsePath);
    }

}
