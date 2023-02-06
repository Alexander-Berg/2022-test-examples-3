package ru.yandex.market.checkout.pushapi.client.json;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.common.json.JsonDeserializer;
import ru.yandex.market.checkout.common.json.JsonReader;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;

import java.io.IOException;

@Component
public class OrderResponseJsonDeserializer implements JsonDeserializer<OrderResponse> {
    @Override
    public OrderResponse deserialize(JsonReader reader) throws IOException {
        final OrderResponse orderResponse = new OrderResponse();
        final JsonReader orderReader = reader.getReader("order");

        orderResponse.setId(orderReader.getString("id"));
        orderResponse.setAccepted(orderReader.getBool("accepted"));
        orderResponse.setReason(orderReader.getEnum("reason", DeclineReason.class));

        return orderResponse;
    }
}
