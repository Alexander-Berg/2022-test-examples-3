package ru.yandex.market.delivery.tracker.client.tracking.lgw.client;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;
import ru.yandex.market.delivery.tracker.service.logger.LgwStatusAndHistoryRequestLogger;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingFulfillmentClientMethodMappingTest {

    private static final Partner PARTNER = new Partner(10L);

    @Mock
    private LgwStatusAndHistoryRequestLogger log;

    @Mock
    private FulfillmentClient fulfillmentClient;

    @InjectMocks
    private LoggingFulfillmentClient loggingFulfillmentClient;

    @BeforeEach
    void setUp() {
        when(log.logRequest(any(), any(), any(), any()))
            .thenAnswer(answer -> ((Supplier<?>) answer.getArguments()[3]).get());
    }

    @Test
    void getInboundHistoryTest() {
        var orderId = ResourceId.builder().build();

        loggingFulfillmentClient.getInboundHistory(orderId, PARTNER);

        verifyLogRequest(RequestType.INBOUND_HISTORY_OLD);
        verify(fulfillmentClient).getInboundHistory(eq(orderId), eq(PARTNER));
    }

    @Test
    void getInboundsStatusTest() {
        var inboundsId = List.of(ResourceId.builder().build());

        loggingFulfillmentClient.getInboundsStatus(inboundsId, PARTNER);

        verifyLogRequest(RequestType.INBOUNDS_STATUS_OLD);
        verify(fulfillmentClient).getInboundsStatus(eq(inboundsId), eq(PARTNER));
    }

    @Test
    void getOutboundHistoryTest() {
        var outboundId = ResourceId.builder().build();

        loggingFulfillmentClient.getOutboundHistory(outboundId, PARTNER);

        verifyLogRequest(RequestType.OUTBOUND_HISTORY_OLD);
        verify(fulfillmentClient).getOutboundHistory(eq(outboundId), eq(PARTNER));
    }

    @Test
    void getOutboundsStatusTest() {
        var outboundsId = List.of(ResourceId.builder().build());

        loggingFulfillmentClient.getOutboundsStatus(outboundsId, PARTNER);

        verifyLogRequest(RequestType.OUTBOUNDS_STATUS_OLD);
        verify(fulfillmentClient).getOutboundsStatus(eq(outboundsId), eq(PARTNER));
    }

    @Test
    void getOrderHistoryTest() {
        var orderId = ResourceId.builder().build();

        loggingFulfillmentClient.getOrderHistory(orderId, PARTNER);

        verifyLogRequest(RequestType.ORDER_HISTORY);
        verify(fulfillmentClient).getOrderHistory(eq(orderId), eq(PARTNER));
    }

    @Test
    void getOrdersStatusTest() {
        var ordersId = List.of(ResourceId.builder().build());

        loggingFulfillmentClient.getOrdersStatus(ordersId, PARTNER);

        verifyLogRequest(RequestType.ORDER_STATUS);
        verify(fulfillmentClient).getOrdersStatus(eq(ordersId), eq(PARTNER));
    }

    @Test
    void getTransferHistoryTest() {
        var transferId = ResourceId.builder().build();

        loggingFulfillmentClient.getTransferHistory(transferId, PARTNER);

        verifyLogRequest(RequestType.TRANSFER_HISTORY);
        verify(fulfillmentClient).getTransferHistory(eq(transferId), eq(PARTNER));
    }

    @Test
    void getTransfersStatusTest() {
        var transfersId = List.of(ResourceId.builder().build());

        loggingFulfillmentClient.getTransfersStatus(transfersId, PARTNER);

        verifyLogRequest(RequestType.TRANSFER_STATUS);
        verify(fulfillmentClient).getTransfersStatus(eq(transfersId), eq(PARTNER));
    }

    @Test
    void getInboundStatusTest() {
        var inboundIds = List.of(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build()
        );

        loggingFulfillmentClient.getInboundStatus(inboundIds, PARTNER);

        verifyLogRequest(RequestType.INBOUND_STATUS);
        verify(fulfillmentClient).getInboundStatus(eq(inboundIds), eq(PARTNER));
    }

    @Test
    void getInboundStatusHistoryTest() {
        var inboundIds = List.of(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build()
        );

        loggingFulfillmentClient.getInboundStatusHistory(inboundIds, PARTNER);

        verifyLogRequest(RequestType.INBOUND_STATUS_HISTORY);
        verify(fulfillmentClient).getInboundStatusHistory(eq(inboundIds), eq(PARTNER));
    }

    @Test
    void getOutboundStatusTest() {
        var outboundIds = List.of(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build()
        );

        loggingFulfillmentClient.getOutboundStatus(outboundIds, PARTNER);

        verifyLogRequest(RequestType.OUTBOUND_STATUS);
        verify(fulfillmentClient).getOutboundStatus(eq(outboundIds), eq(PARTNER));
    }

    @Test
    void getOutboundStatusHistoryTest() {
        var outboundIds = List.of(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build()
        );

        loggingFulfillmentClient.getOutboundStatusHistory(outboundIds, PARTNER);

        verifyLogRequest(RequestType.OUTBOUND_STATUS_HISTORY);
        verify(fulfillmentClient).getOutboundStatusHistory(eq(outboundIds), eq(PARTNER));
    }

    private void verifyLogRequest(RequestType requestType) {
        verify(log).logRequest(
            eq(requestType),
            eq(ApiVersion.FF),
            eq(PARTNER.getId()),
            any()
        );
    }

}
