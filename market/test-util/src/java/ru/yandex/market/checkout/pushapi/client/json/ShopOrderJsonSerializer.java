package ru.yandex.market.checkout.pushapi.client.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.json.JsonSerializer;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.json.order.OrderItemJsonSerializer;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

import java.io.IOException;

@Component
public class ShopOrderJsonSerializer implements JsonSerializer<ShopOrder> {
    
    private OrderItemJsonSerializer itemJsonSerializer;
    private DeliveryWithRegionJsonSerializer deliveryWithRegionJsonSerializer;
    private CheckoutDateFormat checkoutDateFormat;

    @Autowired
    public void setItemJsonSerializer(OrderItemJsonSerializer itemJsonSerializer) {
        this.itemJsonSerializer = itemJsonSerializer;
    }

    @Autowired
    public void setDeliveryWithRegionJsonSerializer(DeliveryWithRegionJsonSerializer deliveryWithRegionJsonSerializer) {
        this.deliveryWithRegionJsonSerializer = deliveryWithRegionJsonSerializer;
    }

    @Autowired
    public void setCheckoutDateFormat(CheckoutDateFormat checkoutDateFormat) {
        this.checkoutDateFormat = checkoutDateFormat;
    }

    @Override
    public void serialize(ShopOrder value, JsonWriter writer) throws IOException {
        writer.startObject();
        writer.startObject("order");
        
        writer.setAttribute("id", value.getId());
        writer.setAttribute("fake", value.isFake());
        if(value.getCurrency() != null) {
            writer.setAttribute("currency", value.getCurrency().name());
        }
        writer.setAttribute("paymentType", value.getPaymentType());
        writer.setAttribute("paymentMethod", value.getPaymentMethod());

        writer.setAttribute("status", value.getStatus());
        writer.setAttribute("substatus", value.getSubstatus());
        writer.setAttribute("creationDate", value.getCreationDate(), checkoutDateFormat.createLongDateFormat());
        writer.setAttribute("itemsTotal", value.getItemsTotal());
        writer.setAttribute("total", value.getTotal());

        if(value.getDeliveryWithRegion() != null) {
            writer.setAttribute("delivery");
            deliveryWithRegionJsonSerializer.serialize(value.getDeliveryWithRegion(), writer);
        }

        final Buyer buyer = value.getBuyer();
        if(buyer != null) {
            writer.startObject("buyer");
            writer.setAttribute("id", buyer.getId());
            writer.setAttribute("lastName", buyer.getLastName());
            writer.setAttribute("firstName", buyer.getFirstName());
            writer.setAttribute("middleName", buyer.getMiddleName());
            writer.setAttribute("phone", buyer.getPhone());
            writer.setAttribute("email", buyer.getEmail());
            writer.endObject();
        }

        if(value.getItems() != null) {
            writer.startArray("items");
            for(OrderItem item: value.getItems()) {
                itemJsonSerializer.serialize(item, writer);
            }
            writer.endArray();
        }

        writer.setAttribute("notes", value.getNotes());

        writer.endObject();
        writer.endObject();
    }
}
