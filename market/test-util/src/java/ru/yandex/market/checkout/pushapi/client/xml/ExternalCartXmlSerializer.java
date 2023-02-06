package ru.yandex.market.checkout.pushapi.client.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.util.AbstractAutowiringXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;

import java.io.IOException;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExternalCartXmlSerializer extends AbstractAutowiringXmlSerializer<ExternalCart> {
    
    private DeliveryWithRegionXmlSerializer deliveryWithRegionXmlSerializer;
    private OrderItemXmlSerializer itemXmlSerializer;

    @Autowired
    public void setDeliveryWithRegionXmlSerializer(DeliveryWithRegionXmlSerializer deliveryWithRegionXmlSerializer) {
        this.deliveryWithRegionXmlSerializer = deliveryWithRegionXmlSerializer;
    }

    @Autowired
    public void setItemXmlSerializer(OrderItemXmlSerializer itemXmlSerializer) {
        this.itemXmlSerializer = itemXmlSerializer;
    }

    @Override
    public void serializeXml(ExternalCart value, PrimitiveXmlWriter writer) throws IOException {
        writer.startNode("cart");

        if(value.getCurrency() != null) {
            writer.setAttribute("currency", value.getCurrency().name());
        }

        if(value.getDeliveryWithRegion() != null) {
            deliveryWithRegionXmlSerializer.serializeXml(value.getDeliveryWithRegion(), writer);
        }

        if(value.getItems() != null) {
            writer.startNode("items");
            for(OrderItem item: value.getItems()) {
                itemXmlSerializer.serializeXml(item, writer);
            }
            writer.endNode();
        }

        writer.endNode();
    }
    
}
