package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class OrderHistoryEventsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        OrderHistoryEvents orderHistoryEvents = new OrderHistoryEvents(
                Collections.singletonList(EntityHelper.getOrderHistoryEvent())
        );

        String json = write(orderHistoryEvents);
        System.out.println(json);

        checkJson(json, "$." + Names.History.EVENTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.History.EVENTS, hasSize(1));
    }

    @Test
    public void deserialize() throws IOException {
        OrderHistoryEvents read = read(OrderHistoryEvents.class, getClass().getResourceAsStream("orderHistoryEvents" +
                ".json"));

        Assertions.assertNotNull(read.getContent());
        assertThat(read.getContent(), hasSize(1));
    }
}
