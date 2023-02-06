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
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ds.LgwDsExternalOrderClientAdapter;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;

import static java.util.Arrays.asList;

public class LgwDsExternalOrderRequestResponseParsingTest extends AbstractLgwParsingTest {
    private static final long SERVICE_ID = 50;
    private static final String EXTERNAL_ORDERS_STATUS_URL = "/delivery/getExternalOrdersStatus";
    private static final String EXTERNAL_ORDER_HISTORY_URL = "/delivery/getExternalOrderHistory";
    private static final String YANDEX_ID = "6528157";
    private static final String PARTNER_ID = "39449271";
    private static final String SECOND_YANDEX_ID = "6459047";
    private static final String SECOND_PARTNER_ID = "39434629";
    private static final String DATE = "2019-05-15T11:18:50+03:00";
    private static final String NEXT_DATE = "2019-05-15T13:09:15+03:00";
    private static final long PIM_PAY_ID = 999L;


    private LgwDsExternalOrderClientAdapter lgwDsExternalOrderClientAdapter;

    @BeforeEach
    void additionalSetup() {
        DeliveryClient deliveryClient = configuration.deliveryClient();
        lgwDsExternalOrderClientAdapter = new LgwDsExternalOrderClientAdapter(deliveryClient);
    }

    @Test
    void dsGetExternalOrderHistory() throws ParseException, IOException {
        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID),
            Arrays.asList(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build(),
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_PASSED_CUSTOMS.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build()
            )
        );

        setupMockServerExpectation(EXTERNAL_ORDER_HISTORY_URL, "ds_get_external_order_history.json");

        TrackerResponse<TrackerHistory> response = lgwDsExternalOrderClientAdapter.getHistory(
            PIM_PAY_ID,
            new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetExternalOrderHistoryDifferentTimezone() throws ParseException, IOException {
        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID),
            Collections.singletonList(
                TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build()
            )
        );

        setupMockServerExpectation(EXTERNAL_ORDER_HISTORY_URL, "ds_get_external_order_history_timezone.json");

        TrackerResponse<TrackerHistory> response = lgwDsExternalOrderClientAdapter.getHistory(
            PIM_PAY_ID,
            new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetExternalOrdersStatuses() throws ParseException, IOException {
        List<TrackerHistory> expectedPayload = Collections.singletonList(
            new TrackerHistory(
                new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build()
                )
            )
        );

        setupMockServerExpectation(EXTERNAL_ORDERS_STATUS_URL, "ds_get_external_orders_status.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsExternalOrderClientAdapter.getStatus(
            PIM_PAY_ID,
            Collections.singletonList(new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID))
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetExternalOrdersStatusesWithPartnerId() throws ParseException, IOException {
        List<TrackerHistory> expectedPayload = Collections.singletonList(
            new TrackerHistory(
                new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build()
                )
            )
        );

        setupMockServerExpectation(EXTERNAL_ORDERS_STATUS_URL, "ds_get_external_orders_status_with_partner_id.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsExternalOrderClientAdapter.getStatus(
            PIM_PAY_ID,
            Collections.singletonList(new TrackerEntityId(YANDEX_ID, PARTNER_ID, SERVICE_ID))
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrdersStatusTwoOrders() throws IOException, ParseException {
        String[] yandexIds = new String[]{YANDEX_ID, SECOND_YANDEX_ID};
        String[] partnerIds = new String[]{PARTNER_ID, SECOND_PARTNER_ID};

        List<TrackerHistory> expectedPayload = asList(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], partnerIds[0], SERVICE_ID),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_CREATED.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1], SERVICE_ID),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_BRANCH.getCode())
                    .date(ParsingUtils.parseDate(NEXT_DATE))
                    .message("Казань")
                    .build())
            )
        );

        setupMockServerExpectation(EXTERNAL_ORDERS_STATUS_URL, "ds_get_external_orders_status_two_orders.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsExternalOrderClientAdapter.getStatus(
            PIM_PAY_ID,
            asList(
                new TrackerEntityId(yandexIds[0], partnerIds[0], SERVICE_ID),
                new TrackerEntityId(yandexIds[1], partnerIds[1], SERVICE_ID)
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void dsGetOrdersStatusOrderNotFound() throws IOException, ParseException {
        String[] yandexIds = new String[]{"99999999", SECOND_YANDEX_ID};
        String[] partnerIds = new String[]{"99999999", SECOND_PARTNER_ID};

        List<TrackerHistory> expectedPayload = asList(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], "", SERVICE_ID),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_NOT_FOUND.getCode())
                    .date(ParsingUtils.parseDate(DATE))
                    .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1], SERVICE_ID),
                Collections.singletonList(TrackerStatus.builder()
                    .code(OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_BRANCH.getCode())
                    .date(ParsingUtils.parseDate(NEXT_DATE))
                    .message("Казань")
                    .build())
            )
        );

        setupMockServerExpectation(EXTERNAL_ORDERS_STATUS_URL, "ds_get_external_orders_status_not_found.json");

        TrackerResponse<List<TrackerHistory>> response = lgwDsExternalOrderClientAdapter.getStatus(
            PIM_PAY_ID,
            asList(
                new TrackerEntityId(yandexIds[0], partnerIds[0], SERVICE_ID),
                new TrackerEntityId(yandexIds[1], partnerIds[1], SERVICE_ID)
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }
}
