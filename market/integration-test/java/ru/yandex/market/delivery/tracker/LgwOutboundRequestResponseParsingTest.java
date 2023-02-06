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
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackingService;
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ff.LgwFfOutboundClientAdapter;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.StatusCode;

class LgwOutboundRequestResponseParsingTest extends AbstractLgwParsingTest {

    private static final long SERVICE_ID = 145L;
    private static final String FF_HISTORY_URL = "/fulfillment/getOutboundStatusHistory";
    private static final String FF_STATUS_URL = "/fulfillment/getOutboundStatus";

    private LgwFfOutboundClientAdapter lgwFfOutboundClientAdapter;

    @BeforeEach
    void additionalSetup() {
        FulfillmentClient fulfillmentClient = configuration.fulfillmentClient();

        lgwFfOutboundClientAdapter = new LgwFfOutboundClientAdapter(fulfillmentClient);
    }

    @Test
    void ffGetOutboundStatus() throws ParseException, IOException {
        testGetOutboundStatus(lgwFfOutboundClientAdapter, FF_STATUS_URL);
    }

    @Test
    void ffGetOutboundStatusHistory() throws ParseException, IOException {
        testGetOutboundStatusHistory(lgwFfOutboundClientAdapter, FF_HISTORY_URL);
    }

    private void testGetOutboundStatus(TrackingService trackingService, String statusUrl)
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
                        .code(StatusCode.TRANSFERRED.getCode())
                        .date(ParsingUtils.parseDate("2021-04-20T16:45:00+04:00"))
                        .build()
                )
            )
        );

        setupMockServerExpectation(statusUrl, "get_outbound_status.json");

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

    void testGetOutboundStatusHistory(TrackingService trackingService, String historyUrl)
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
                    .code(StatusCode.ASSEMBLING.getCode())
                    .date(ParsingUtils.parseDate("2020-04-21T21:05:00+03:00"))
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.TRANSFERRED.getCode())
                    .date(ParsingUtils.parseDate("2020-04-20T12:00:00+03:00"))
                    .build()
            )
        );

        setupMockServerExpectation(historyUrl, "get_outbound_status_history.json");

        TrackerResponse<TrackerHistory> response = trackingService.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }
}
