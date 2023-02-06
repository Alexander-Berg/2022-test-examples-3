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
import ru.yandex.market.delivery.tracker.client.tracking.lgw.ff.LgwFfOutboundOldClientAdapter;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;

class LgwFfOutboundOldRequestResponseParsingTest extends AbstractLgwParsingTest {

    private static final long SERVICE_ID = 145L;
    private static final String HISTORY_URL = "/fulfillment/getOutboundHistory";
    private static final String STATUS_URL = "/fulfillment/getOutboundsStatus";

    private LgwFfOutboundOldClientAdapter lgwFfOutboundOldClientAdapter;

    @BeforeEach
    void additionalSetup() {
        FulfillmentClient fulfillmentClient = configuration.fulfillmentClient();
        lgwFfOutboundOldClientAdapter = new LgwFfOutboundOldClientAdapter(fulfillmentClient);
    }

    @Test
    void ffGetOutboundsStatusOld() throws ParseException, IOException {
        String[] yandexIds = new String[]{"543242", "543245"};
        String[] partnerIds = new String[]{"AD156DF", "AD156D5"};

        List<TrackerHistory> expectedPayload = List.of(
            new TrackerHistory(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.ASSEMBLING.getCode())
                        .date(ParsingUtils.parseDate("2019-01-10T18:42:59+03:00"))
                        .build()
                )
            ),
            new TrackerHistory(
                new TrackerEntityId(yandexIds[1], partnerIds[1]),
                List.of(
                    TrackerStatus.builder()
                        .code(StatusCode.ASSEMBLING.getCode())
                        .date(ParsingUtils.parseDate("2019-01-10T18:42:59+03:00"))
                        .build()
                )
            )
        );

        setupMockServerExpectation(STATUS_URL, "ff_get_outbounds_status_old.json");

        TrackerResponse<List<TrackerHistory>> response = lgwFfOutboundOldClientAdapter.getStatus(
            SERVICE_ID,
            List.of(
                new TrackerEntityId(yandexIds[0], partnerIds[0]),
                new TrackerEntityId(yandexIds[1], partnerIds[1])
            )
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }

    @Test
    void ffGetOutboundHistoryOld() throws ParseException, IOException {
        String yandexId = "500";
        String partnerId = "T5151T";

        TrackerHistory expectedPayload = new TrackerHistory(
            new TrackerEntityId(yandexId, partnerId),
            List.of(
                TrackerStatus.builder()
                    .code(StatusCode.ASSEMBLING.getCode())
                    .date(ParsingUtils.parseDate("2019-01-10T18:42:59+03:00"))
                    .build()
            )
        );

        setupMockServerExpectation(HISTORY_URL, "ff_get_outbound_history_old.json");

        TrackerResponse<TrackerHistory> response = lgwFfOutboundOldClientAdapter.getHistory(
            SERVICE_ID,
            new TrackerEntityId(yandexId, partnerId)
        );

        assertResponseWithExpectedPayload(response, expectedPayload);
    }
}
