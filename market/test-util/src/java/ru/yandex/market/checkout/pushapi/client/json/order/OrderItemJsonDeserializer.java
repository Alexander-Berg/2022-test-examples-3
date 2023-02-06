package ru.yandex.market.checkout.pushapi.client.json.order;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.json.JsonDeserializer;
import ru.yandex.market.checkout.common.json.JsonReader;

import java.io.IOException;

@Component
public class OrderItemJsonDeserializer implements JsonDeserializer<OrderItem> {

    @Override
    public OrderItem deserialize(JsonReader reader) throws IOException {
        final OrderItem item = new OrderItem();
        item.setFeedId(reader.getLong("feedId"));
        item.setOfferId(reader.getString("offerId"));
        item.setFeedCategoryId(reader.getString("feedCategoryId"));
        item.setOfferName(reader.getString("offerName"));
        item.setPrice(reader.getBigDecimal("price"));
        item.setCount(reader.getInt("count"));
        item.setDelivery(reader.getBool("delivery"));
        return item;
    }
}
