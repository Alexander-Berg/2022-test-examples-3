package ru.yandex.market.util;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.pushapi.client.util.AbstractAutowiringXmlSerializer;
import ru.yandex.market.shopadminstub.model.OrderAcceptRequest;

import java.io.IOException;

@Component
public class OrderAcceptRequestSerializer extends AbstractAutowiringXmlSerializer<OrderAcceptRequest> {
    @Override
    public void serializeXml(OrderAcceptRequest value, PrimitiveXmlWriter writer) throws IOException {
        writer.startNode("order");
        writer.setAttribute("id", value.getId());
        writer.setAttribute("payment-type", value.getPaymentType());
        writer.setAttribute("payment-method", value.getPaymentMethod());
        writer.setAttribute("fulfilment", value.isFulfilment());
        writer.setAttribute("preorder", value.isPreorder());

        ItemsSerializer.writeItems(writer, value.getItems());
        DeliverySerializer.write(writer, value.getDelivery());

        writer.endNode();
    }
}
