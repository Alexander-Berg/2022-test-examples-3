package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.MoveOrderResponse;
import ru.yandex.market.checkout.checkouter.order.MoveOrderStatus;

public class MoveOrderResponseHandlerTest extends AbstractJsonHandlerTestBase {

    private static final int ORDER_ID = 123;
    private static final MoveOrderStatus STATUS = MoveOrderStatus.SUCCESS;

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"id\": 123, \"status\": \"SUCCESS\"}";

        MoveOrderResponse response = read(MoveOrderResponse.class, json);

        Assertions.assertEquals(123L, response.getOrderId());
        Assertions.assertEquals(MoveOrderStatus.SUCCESS, response.getStatus());
    }

    @Test
    public void serialize() throws Exception {
        MoveOrderResponse response = new MoveOrderResponse(ORDER_ID, STATUS);

        String json = write(response);

        checkJson(json, "$." + Names.MoveOrders.ID, ORDER_ID);
        checkJson(json, "$." + Names.MoveOrders.STATUS, MoveOrderStatus.SUCCESS.name());
    }

}
