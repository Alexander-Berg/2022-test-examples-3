package ru.yandex.market.delivery.transport_manager.service.external.lgw;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Movement;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup({
    "/repository/transportation/multiple_transportation_partner_info.xml",
    "/repository/transportation_partner_method/success_without_api_type.xml"
})
class LgwClientExecutorTest extends AbstractContextualTest {

    @Autowired
    private LgwClientExecutor lgwClientExecutor;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @BeforeEach
    void init() throws GatewayApiException {
        doNothing().when(deliveryClient).putMovement(any(Movement.class), any(Partner.class));
        doNothing().when(fulfillmentClient).cancelInbound(any(), any());
    }

    @Test
    void testSuccessPutMovement() throws GatewayApiException {
        lgwClientExecutor.putMovement(movement(), new Partner(51L));

        verify(deliveryClient).putMovement(eq(movement()), any(Partner.class));
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    void testSuccessCancelInbound() throws GatewayApiException {
        var partner = new Partner(164L);
        lgwClientExecutor.cancelInbound(cancelResourceId(), partner);

        verify(fulfillmentClient).cancelInbound(cancelResourceId(), partner);
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    void testSuccessCancelOutbound() throws GatewayApiException {
        var partner = new Partner(4L);
        lgwClientExecutor.cancelOutbound(cancelResourceId(), partner);

        verify(fulfillmentClient).cancelOutbound(cancelResourceId(), partner);
        verifyNoMoreInteractions(deliveryClient);
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId cancelResourceId() {
        return ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder().build();
    }

    private Movement movement() {
        return Movement
            .builder(
                ResourceId.builder().build(),
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00"),
                BigDecimal.valueOf(1)
            )
            .build();
    }
}
