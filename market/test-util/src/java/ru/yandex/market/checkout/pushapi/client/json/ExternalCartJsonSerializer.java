package ru.yandex.market.checkout.pushapi.client.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.json.JsonSerializer;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.json.order.OrderItemJsonSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;

import java.io.IOException;

@Component
public class ExternalCartJsonSerializer implements JsonSerializer<ExternalCart> {

    private DeliveryWithRegionJsonSerializer deliveryWithRegionJsonSerializer;
    private OrderItemJsonSerializer itemJsonSerializer;

    @Autowired
    public void setDeliveryWithRegionJsonSerializer(DeliveryWithRegionJsonSerializer deliveryWithRegionJsonSerializer) {
        this.deliveryWithRegionJsonSerializer = deliveryWithRegionJsonSerializer;
    }

    @Autowired
    public void setItemJsonSerializer(OrderItemJsonSerializer itemJsonSerializer) {
        this.itemJsonSerializer = itemJsonSerializer;
    }

    @Override
    public void serialize(ExternalCart value, JsonWriter writer) throws IOException {
        writer.startObject();
        writer.startObject("cart");

        if(value.getCurrency() != null) {
            writer.setAttribute("currency", value.getCurrency().name());
        }

        if(value.getItems() != null) {
            writer.startArray("items");
            for(OrderItem item: value.getItems()) {
                itemJsonSerializer.serialize(item, writer);
            }
            writer.endArray();
        }

        if(value.getDeliveryWithRegion() != null) {
            writer.setAttribute("delivery");
            deliveryWithRegionJsonSerializer.serialize(value.getDeliveryWithRegion(), writer);
        }

        writer.endObject();
        writer.endObject();
    }
    
}
