package ru.yandex.market.delivery.tracker;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerEntityId;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerHistory;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerResponse;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerStatus;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackingService;
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ds.LgwDsOrderReturnClientAdapter;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.StatusCode;

@Disabled("Используются неподходящие файлы из существующих тестов, надо переписывать")
class LgwOrderReturnRequestResponseParsingTest extends AbstractLgwParsingTest {

    private static final long SERVICE_ID = 145L;
    private static final String DS_HISTORY_URL = "/delivery/getOrderHistory";
    private static final String DS_STATUS_URL = "/delivery/getOrdersStatus";

    private LgwDsOrderReturnClientAdapter lgwDsOrderReturnClientAdapter;

    @BeforeEach
    void additionalSetup() {
        DeliveryClient deliveryClient = configuration.deliveryClient();

        lgwDsOrderReturnClientAdapter = new LgwDsOrderReturnClientAdapter(deliveryClient);
    }

    @Test
    void dsGetOrderReturnStatus() throws ParseException, IOException {
        testGetOrderReturnStatus(lgwDsOrderReturnClientAdapter);
    }

    @Test
    void dsGetOrderReturnStatusHistory() throws ParseException, IOException {
        testGetOrderReturnHistory(lgwDsOrderReturnClientAdapter);
    }

    private void testGetOrderReturnStatus(TrackingService trackingService)
        throws ParseException, IOException {

        String[] yandexIds = new String[]{"111424", "222424", "333424"};
        String[] partnerIds = new String[]{"111525", "222525", "333525"};

        List<TrackerHistory> expectedPayload = List.of(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.CREATED.getCode())
                        .date(ParsingUtils.parseDate("2020-03-20T12:00:00+01:00"))
                        .message("Some message")
                        .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.ERROR.getCode())
                        .date(ParsingUtils.parseDate("2020-05-10T09:00:00+03:00"))
                        .message("Another message")
                        .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[2], partnerIds[2]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.DELIVERED.getCode())
                        .date(ParsingUtils.parseDate("2021-04-20T16:45:00+04:00"))
                        .build()
                )
            )
        );

        setupMockServerExpectation(LgwOrderReturnRequestResponseParsingTest.DS_STATUS_URL, "ds_get_orders_status.json");

        TrackerResponse<List<TrackerHistory>> response = trackingService.getStatus(
            SERVICE_ID,
            List.of(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                new TrackerEntityId(yandexIds[2], partnerIds[2])
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    private void testGetOrderReturnHistory(TrackingService trackingService)
        throws ParseException, IOException {

        String yandexId = "333424";
        String partnerId = "333525";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            List.of(
                TrackerStatus.builder()
                    .code(StatusCode.PENDING.getCode())
                    .date(ParsingUtils.parseDate("2020-04-20T09:00:00+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.CREATED.getCode())
                    .date(ParsingUtils.parseDate("2020-04-20T10:00:00+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.COURIER_FOUND.getCode())
                    .date(ParsingUtils.parseDate("2020-04-20T14:00:00+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.HANDED_OVER.getCode())
                    .date(ParsingUtils.parseDate("2020-04-20T14:17:00+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.DELIVERING.getCode())
                    .date(ParsingUtils.parseDate("2020-04-21T21:05:00+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.DELIVERED.getCode())
                    .date(ParsingUtils.parseDate("2020-04-20T12:00:00+03:00"))
                    .build()
            )
        );

        setupMockServerExpectation(LgwOrderReturnRequestResponseParsingTest.DS_HISTORY_URL,
            "ds_get_orders_history.json");

        TrackerResponse<TrackerHistory> response = trackingService.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }
}
