package ru.yandex.market.delivery.tracker;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerEntityId;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerHistory;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerResponse;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerStatus;
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ds.LgwDsOrderClientAdapter;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;

import static java.util.Arrays.asList;

class LgwDsOrderRequestResponseParsingTest extends AbstractLgwParsingTest {
    private static final long SERVICE_ID = 50L;
    private static final String ORDERS_STATUS_URL = "/delivery/getOrdersStatus";
    private static final String ORDER_HISTORY_URL = "/delivery/getOrderHistory";

    private LgwDsOrderClientAdapter lgwDsOrderClientAdapter;

    @BeforeEach
    void additionalSetup() {
        DeliveryClient deliveryClient = configuration.deliveryClient();
        lgwDsOrderClientAdapter = new LgwDsOrderClientAdapter(deliveryClient);
    }

    @Test
    void dsGetOrderHistory() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "39449271";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            Arrays.asList(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:18:50+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_PASSED_CUSTOMS.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:18:50+03:00"))
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ds_get_order_history.json");

        TrackerResponse<TrackerHistory> response = lgwDsOrderClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrderHistoryWithWarehouse() throws ParseException, IOException {
        String yandexId = "4497456";
        String partnerId = "14081936030289";
        int deliveryServiceId = 123;

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            Collections.singletonList(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_ARRIVED_TO_PICKUP_POINT.getCode())
                    .date(ParsingUtils.parseDate("2019-06-11T11:49:34+03:00"))
                    .zipCode("446600")
                    .location("Нефтегорск")
                    .country("Российская Федерация")
                    .message("Обработка(8):Прибыло в место вручения(2) [Нефтегорск]")
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ds_get_order_history_with_warehouse.json");

        TrackerResponse<TrackerHistory> response = lgwDsOrderClientAdapter.getHistory(
            deliveryServiceId,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrderHistoryWithCity() throws IOException, ParseException {
        String yandexId = "4031866";
        String partnerId = "RZ021197780LV";
        int deliveryServiceId = 154;

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            Collections.singletonList(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_DELIVERED_TO_RECIPIENT.getCode())
                    .date(ParsingUtils.parseDate("2019-06-12T00:41:00+03:00"))
                    .city("Riga")
                    .message("EMI")
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ds_get_order_history_with_city.json");

        TrackerResponse<TrackerHistory> response = lgwDsOrderClientAdapter.getHistory(
            deliveryServiceId,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrderHistoryDifferentTimezone() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "39449271";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            Collections.singletonList(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:18:50+03:00"))
                    .build()
            )
        );

        setupMockServerExpectation(ORDER_HISTORY_URL, "ds_get_order_history_timezone.json");

        TrackerResponse<TrackerHistory> response = lgwDsOrderClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrdersStatuses() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "39449271";

        List<TrackerHistory> expectedPayload = Collections.singletonList(
            new TrackerHistory(
                new TrackerEntityId(yandexId, partnerId),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:18:50+03:00"))
                    .build()
                )
            )
        );

        setupMockServerExpectation(ORDERS_STATUS_URL, "ds_get_orders_status.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsOrderClientAdapter.getStatus(
            SERVICE_ID,
            Collections.singletonList(new TrackerEntityId(yandexId, partnerId))
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrdersStatusesWithPartnerId() throws ParseException, IOException {
        String yandexId = "6528157";
        String partnerId = "39449271";

        List<TrackerHistory> expectedPayload = Collections.singletonList(
            new TrackerHistory(
                new TrackerEntityId(yandexId, partnerId),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:18:50+03:00"))
                    .build()
                )
            )
        );

        setupMockServerExpectation(ORDERS_STATUS_URL, "ds_get_orders_status_with_partner_id.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsOrderClientAdapter.getStatus(
            SERVICE_ID,
            Collections.singletonList(new TrackerEntityId(yandexId, partnerId))
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrdersStatusTwoOrders() throws IOException, ParseException {
        String[] yandexIds = new String[]{"6528157", "6459047"};
        String[] partnerIds = new String[]{"39449271", "39434629"};

        List<TrackerHistory> expectedPayload = asList(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T11:18:50+03:00"))
                    .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_BRANCH.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T13:09:15+03:00"))
                    .message("Казань")
                    .build())
            )
        );

        setupMockServerExpectation(ORDERS_STATUS_URL, "ds_get_orders_status_two_orders.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsOrderClientAdapter.getStatus(
            SERVICE_ID,
            asList(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                new TrackerEntityId(yandexIds[1], partnerIds[1])
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrdersStatusOrderNotFound() throws IOException, ParseException {
        String[] yandexIds = new String[]{"99999999", "6459047"};
        String[] partnerIds = new String[]{"99999999", "39434629"};

        List<TrackerHistory> expectedPayload = asList(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], ""),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_NOT_FOUND.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T14:53:48+03:00"))
                    .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_BRANCH.getCode())
                    .date(ParsingUtils.parseDate("2019-05-15T13:09:15+03:00"))
                    .message("Казань")
                    .build())
            )
        );

        setupMockServerExpectation(ORDERS_STATUS_URL, "ds_get_orders_status_not_found.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsOrderClientAdapter.getStatus(
            SERVICE_ID,
            asList(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                new TrackerEntityId(yandexIds[1], partnerIds[1])
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

}
