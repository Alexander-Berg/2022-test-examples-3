package ru.yandex.market.delivery.tracker;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerEntityId;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerHistory;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerResponse;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerStatus;
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ff.LgwFfInboundOldClientAdapter;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;

class LgwFfInboundOldRequestResponseParsingTest extends AbstractLgwParsingTest {

    private static final long SERVICE_ID = 145L;
    private static final String HISTORY_URL = "/fulfillment/getInboundHistory";
    private static final String STATUS_URL = "/fulfillment/getInboundsStatus";

    private LgwFfInboundOldClientAdapter lgwFfInboundOldClientAdapter;

    @BeforeEach
    void additionalSetup() {
        FulfillmentClient fulfillmentClient = configuration.fulfillmentClient();
        lgwFfInboundOldClientAdapter = new LgwFfInboundOldClientAdapter(fulfillmentClient);
    }

    @Test
    void ffGetInboundsStatusOld() throws ParseException, IOException {
        String[] yandexIds = new String[]{"543242", "543243"};
        String[] partnerIds = new String[]{"AD156DF", "AD156DD"};

        List<TrackerHistory> expectedPayload = List.of(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.ACCEPTANCE.getCode())
                        .date(ParsingUtils.parseDate("2019-01-17T13:54:59+03:00"))
                        .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.ARRIVED.getCode())
                        .date(ParsingUtils.parseDate("2018-12-24T15:15:59+03:00"))
                        .build()
                )
            )
        );

        setupMockServerExpectation(STATUS_URL, "ff_get_inbounds_status_old.json");

        TrackerResponse<List<TrackerHistory>> response = lgwFfInboundOldClientAdapter.getStatus(
            SERVICE_ID,
            List.of(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                new TrackerEntityId(yandexIds[1], partnerIds[1])
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void ffGetInboundHistoryOld() throws ParseException, IOException {
        String yandexId = "400";
        String partnerId = "Z342";
        Date date = ParsingUtils.parseDate("2018-12-21T11:59:59+03:00");
        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            List.of(
                TrackerStatus.builder()
                    .code(StatusCode.ACCEPTED.getCode())
                    .date(date)
                    .build(),
                TrackerStatus.builder()
                    .code(StatusCode.CANCELLED.getCode())
                    .date(date)
                    .build()
            )
        );

        setupMockServerExpectation(HISTORY_URL, "ff_get_inbound_history_old.json");

        TrackerResponse<TrackerHistory> response = lgwFfInboundOldClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }
}
