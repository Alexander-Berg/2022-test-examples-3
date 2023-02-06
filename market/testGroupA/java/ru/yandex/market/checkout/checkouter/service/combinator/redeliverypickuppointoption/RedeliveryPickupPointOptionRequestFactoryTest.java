package ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption.RedeliveryPickupPointOptionDummyUtils.getRequest;
import static ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption.RedeliveryPickupPointOptionRequestFactory.buildRequest;

public class RedeliveryPickupPointOptionRequestFactoryTest {

    @Test
    public void buildRequestTest() {
        // Assign
        var order = new Order();
        order.setId(100500L);
        order.setProperty(OrderPropertyType.EXPERIMENTS, "factor1=value1;factor2=value2");

        var expectedRequest = getRequest();

        // Act
        var request = buildRequest(order, 200400L);

        // Assert
        assertEquals(expectedRequest, request);
    }

}
