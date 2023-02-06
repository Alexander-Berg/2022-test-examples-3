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
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingDeliveryClientMethodMappingTest {

    private static final Partner PARTNER = new Partner(10L);

    @Mock
    private LgwStatusAndHistoryRequestLogger log;

    @Mock
    private DeliveryClient deliveryClient;

    @InjectMocks
    private LoggingDeliveryClient loggingDeliveryClient;

    @BeforeEach
    void setUp() {
        when(log.logRequest(any(), any(), any(), any()))
            .thenAnswer(answer -> ((Supplier<?>) answer.getArguments()[3]).get());
    }

    @Test
    void getOrderHistoryTest() {
        var orderId = ResourceId.builder().build();

        loggingDeliveryClient.getOrderHistory(orderId, PARTNER);

        verifyLogRequest(RequestType.ORDER_HISTORY);
        verify(deliveryClient).getOrderHistory(eq(orderId), eq(PARTNER));
    }

    @Test
    void getExternalOrderHistoryTest() {
        var orderId = new ExternalResourceId("", "", "");

        loggingDeliveryClient.getExternalOrderHistory(orderId, PARTNER);

        verifyLogRequest(RequestType.EXTERNAL_ORDER_HISTORY);
        verify(deliveryClient).getExternalOrderHistory(eq(orderId), eq(PARTNER));
    }

    @Test
    void getExternalOrdersStatusTest() {
        var ordersId = List.of(new ExternalResourceId("", "", ""));

        loggingDeliveryClient.getExternalOrdersStatus(ordersId, PARTNER);

        verifyLogRequest(RequestType.EXTERNAL_ORDER_STATUS);
        verify(deliveryClient).getExternalOrdersStatus(eq(ordersId), eq(PARTNER));
    }

    @Test
    void getOrdersStatusTest() {
        var ordersId = List.of(ResourceId.builder().build());

        loggingDeliveryClient.getOrdersStatus(ordersId, PARTNER);

        verifyLogRequest(RequestType.ORDER_STATUS);
        verify(deliveryClient).getOrdersStatus(eq(ordersId), eq(PARTNER));
    }

    @Test
    void getMovementStatusTest() {
        var movementIds = List.of(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build()
        );

        loggingDeliveryClient.getMovementStatus(movementIds, PARTNER);

        verifyLogRequest(RequestType.MOVEMENT_STATUS);
        verify(deliveryClient).getMovementStatus(eq(movementIds), eq(PARTNER));
    }

    @Test
    void getMovementStatusHistoryTest() {
        var movementIds = List.of(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build()
        );

        loggingDeliveryClient.getMovementStatusHistory(movementIds, PARTNER);

        verifyLogRequest(RequestType.MOVEMENT_STATUS_HISTORY);
        verify(deliveryClient).getMovementStatusHistory(eq(movementIds), eq(PARTNER));
    }

    private void verifyLogRequest(RequestType requestType) {
        verify(log).logRequest(
            eq(requestType),
            eq(ApiVersion.DS),
            eq(PARTNER.getId()),
            any()
        );
    }
}
