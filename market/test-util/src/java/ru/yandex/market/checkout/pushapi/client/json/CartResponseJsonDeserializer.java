package ru.yandex.market.checkout.pushapi.client.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.json.JsonDeserializer;
import ru.yandex.market.checkout.common.json.JsonReader;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.json.order.OrderItemJsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartResponseJsonDeserializer implements JsonDeserializer<CartResponse> {
    
    private DeliveryResponseJsonDeserializer deliveryResponseJsonDeserializer;
    private OrderItemJsonDeserializer orderItemJsonDeserializer;

    @Autowired
    public void setDeliveryResponseJsonDeserializer(DeliveryResponseJsonDeserializer deliveryResponseJsonDeserializer) {
        this.deliveryResponseJsonDeserializer = deliveryResponseJsonDeserializer;
    }

    @Autowired
    public void setOrderItemJsonDeserializer(OrderItemJsonDeserializer orderItemJsonDeserializer) {
        this.orderItemJsonDeserializer = orderItemJsonDeserializer;
    }

    @Override
    public CartResponse deserialize(JsonReader reader) throws IOException {
        final CartResponse cartResponse = new CartResponse();

        final JsonReader cart = reader.getReader("cart");

        final List<JsonReader> itemsReaders = cart.getReaderList("items");
        if(itemsReaders != null) {
            cartResponse.setItems(new ArrayList<OrderItem>());
            for(JsonReader jsonReader : itemsReaders) {
                cartResponse.getItems().add(orderItemJsonDeserializer.deserialize(jsonReader));
            }
        }

        final List<JsonReader> readerList = cart.getReaderList("deliveryOptions");
        if(readerList != null) {
            cartResponse.setDeliveryOptions(new ArrayList<DeliveryResponse>());
            for(JsonReader jsonReader : readerList) {
                cartResponse.getDeliveryOptions().add(deliveryResponseJsonDeserializer.deserialize(jsonReader));
            }
        }

        cartResponse.setPaymentMethods(cart.getEnumList("paymentMethods", PaymentMethod.class));

        return cartResponse;
    }
}
