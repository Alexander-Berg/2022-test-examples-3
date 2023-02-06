package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryUserReceivedRequestBody;

public class DeliveryUserReceivedRequestBodyDeserializerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"userReceived\": true }";

        DeliveryUserReceivedRequestBody deliveryUserReceivedRequestBody = read(DeliveryUserReceivedRequestBody.class,
                json);
        Assertions.assertTrue(deliveryUserReceivedRequestBody.getUserReceived());
    }

}
