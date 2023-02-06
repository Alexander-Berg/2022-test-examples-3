package ru.yandex.market.ff.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Status;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SyncXDocRequestStatusesServiceTest extends AbstractSyncRequestStatusesServiceTest {

    private static final Long REQ_ID_1 = 1L;
    private static final Long REQ_ID_2 = 2L;

    private static final String REQ_EXT_ID_1 = "111";
    private static final String REQ_EXT_ID_2 = "222";

    private static final ResourceId RES_ID_1 = ResourceId.builder()
        .setYandexId(REQ_ID_1.toString())
        .setPartnerId(REQ_EXT_ID_1)
        .build();
    private static final ResourceId RES_ID_2 = ResourceId.builder()
        .setYandexId(REQ_ID_2.toString())
        .setPartnerId(REQ_EXT_ID_2)
        .build();

    private static final Long SERVICE_ID_1 = 666L;

    private static final Partner PARTNER_1 = new Partner(SERVICE_ID_1);

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @BeforeEach
    void init() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, "2000-01-07T00:00:00");
        InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.SHIPPED, "2000-01-08T00:00:00");

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
            .thenReturn(Arrays.asList(status1, status2));

        // История первой заявки.
        InboundStatusHistory inboundHistory = new InboundStatusHistory(
            Collections.singletonList(status(StatusCode.ARRIVED, "2000-01-07T00:00:00")),
            RES_ID_1
        );
        InboundStatusHistory inboundHistory2 = new InboundStatusHistory(
            Collections.singletonList(status(StatusCode.SHIPPED, "2000-01-08T00:00:00")),
            RES_ID_1
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_1, PARTNER_1))
            .thenReturn(inboundHistory);
        when(fulfillmentClient.getInboundHistory(RES_ID_2, PARTNER_1))
            .thenReturn(inboundHistory2);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-xdoc-statuses/before.xml")
    @ExpectedDatabase(value = "classpath:service/sync-xdoc-statuses/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testExecute() {
        syncRequests(Set.of(REQ_ID_1, REQ_ID_2), true);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-xdoc-statuses/before-with-different-date-at-status-history-and-request.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-xdoc-statuses/before-with-different-date-at-status-history-and-request.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void testShouldNotTryingToUpdateHistoryIfSameLastStatusDifferentUpdatedAt() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.CREATED, "2000-01-06T00:00:00");

        when(fulfillmentClient.getInboundsStatus(Collections.singletonList(RES_ID_1), PARTNER_1))
                .thenReturn(Collections.singletonList(status1));

        syncRequests(Set.of(REQ_ID_1), true);

        verify(fulfillmentClient).getInboundsStatus(Collections.singletonList(RES_ID_1), PARTNER_1);
        verifyNoMoreInteractions(fulfillmentClient);
    }

    private static InboundStatus inboundStatus(
        final ResourceId resourceId, final StatusCode statusCode, final String dateTime) {

        return new InboundStatus(resourceId, new Status(statusCode, new DateTime(dateTime)));
    }

    private static Status status(final StatusCode statusCode, final String dateTime) {
        return new Status(statusCode, new DateTime(dateTime));
    }

}
