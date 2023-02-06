package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class OrderIdsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws IOException {
        String json = "{ \"orders\": [ 1, 3, 5 ] }";

        OrderIds orderIds = read(OrderIds.class, json);

        assertThat(orderIds.getContent(), hasItems(1L, 3L, 5L));
    }
}
