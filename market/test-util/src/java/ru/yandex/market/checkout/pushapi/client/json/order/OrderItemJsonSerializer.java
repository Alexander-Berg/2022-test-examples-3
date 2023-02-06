package ru.yandex.market.checkout.pushapi.client.json.order;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.json.JsonSerializer;
import ru.yandex.market.checkout.common.json.JsonWriter;

import java.io.IOException;

@Component
public class OrderItemJsonSerializer implements JsonSerializer<OrderItem> {
    
    @Override
    public void serialize(OrderItem value, JsonWriter generator) throws IOException {
        generator.startObject();

        generator.setAttribute("feedId", value.getFeedId());
        generator.setAttribute("offerId", value.getOfferId());
        generator.setAttribute("feedCategoryId", value.getFeedCategoryId());
        generator.setAttribute("offerName", value.getOfferName());
        generator.setAttribute("price", value.getPrice());
        generator.setAttribute("count", value.getCount());
        generator.setAttribute("delivery", value.getDelivery());

        generator.endObject();
    }
}
