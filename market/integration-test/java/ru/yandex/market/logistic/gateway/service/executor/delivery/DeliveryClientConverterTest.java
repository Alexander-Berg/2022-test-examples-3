package ru.yandex.market.logistic.gateway.service.executor.delivery;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderRequest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionRequestFormatException;
import ru.yandex.market.logistic.gateway.service.executor.delivery.sync.UpdateOrderRequestExecutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class DeliveryClientConverterTest extends AbstractIntegrationTest {

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @Autowired
    private UpdateOrderRequestExecutor executor;

    private final static long TEST_PARTNER_ID = 145L;

    @Test
    public void testRequestValidationFailed() throws Exception {
        UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest(
            buildInvalidOrder(), new Partner(TEST_PARTNER_ID));

        assertThatThrownBy(() -> executor.tryExecute(updateOrderRequest, Collections.emptySet()))
            .isInstanceOf(ServiceInteractionRequestFormatException.class);
    }

    private Order buildInvalidOrder() {
        return new Order.OrderBuilder(
            null, null, null, null, null, null,
            null, null, null, null, null, null,
            null).build();
    }

}
