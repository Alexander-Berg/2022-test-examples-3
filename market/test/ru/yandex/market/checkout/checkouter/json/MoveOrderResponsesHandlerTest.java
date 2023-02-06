package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.MoveOrderResponses;
import ru.yandex.market.checkout.checkouter.order.MoveOrderStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class MoveOrderResponsesHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws IOException {
        String json = "[" +
                "{" +
                "\"id\": 1," +
                "\"status\": \"SUCCESS\"" +
                "}, " +
                "{" +
                "\"id\": 2," +
                "\"status\": \"FAIL\"" +
                "}]";

        MoveOrderResponses moveOrderResponses = read(MoveOrderResponses.class, json);

        assertThat(moveOrderResponses.getItems(), hasSize(2));
        Assertions.assertEquals(1L, moveOrderResponses.getItems().get(0).getOrderId());
        Assertions.assertEquals(MoveOrderStatus.SUCCESS, moveOrderResponses.getItems().get(0).getStatus());
        Assertions.assertEquals(2L, moveOrderResponses.getItems().get(1).getOrderId());
        Assertions.assertEquals(MoveOrderStatus.FAIL, moveOrderResponses.getItems().get(1).getStatus());
    }
}
