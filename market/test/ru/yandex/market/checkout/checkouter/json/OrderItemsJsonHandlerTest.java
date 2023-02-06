package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.OrderItems;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class OrderItemsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        OrderItems items = new OrderItems(Collections.singletonList(EntityHelper.getOrderItem()));

        String json = write(items);
        System.out.println(json);

        checkJson(json, "$.items", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$.items", hasSize(1));
    }

    @Test
    public void deserialize() throws IOException {
        OrderItems orderItems = read(OrderItems.class, getClass().getResourceAsStream("orderItems.json"));

        assertThat(orderItems.getContent(), hasSize(1));
    }
}
