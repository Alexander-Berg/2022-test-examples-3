package ru.yandex.market.checkout.pushapi.client.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.util.AbstractAutowiringXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

import java.io.IOException;

@Component
public class ShopOrderXmlSerializer extends AbstractAutowiringXmlSerializer<ShopOrder> {

    private DeliveryWithRegionXmlSerializer deliveryWithRegionXmlSerializer;
    private OrderItemXmlSerializer orderItemXmlSerializer;
    private CheckoutDateFormat checkoutDateFormat;

    @Autowired
    public void setDeliveryWithRegionXmlSerializer(DeliveryWithRegionXmlSerializer deliveryWithRegionXmlSerializer) {
        this.deliveryWithRegionXmlSerializer = deliveryWithRegionXmlSerializer;
    }

    @Autowired
    public void setOrderItemXmlSerializer(OrderItemXmlSerializer orderItemXmlSerializer) {
        this.orderItemXmlSerializer = orderItemXmlSerializer;
    }

    @Autowired
    public void setCheckoutDateFormat(CheckoutDateFormat checkoutDateFormat) {
        this.checkoutDateFormat = checkoutDateFormat;
    }

    @Override
    public void serializeXml(ShopOrder value, PrimitiveXmlWriter writer) throws IOException {
        writer.startNode("order");

        writer.setAttribute("id", value.getId());
        writer.setAttribute("fake", value.isFake());
        if(value.getCurrency() != null) {
            writer.setAttribute("currency", value.getCurrency().name());
        }
        writer.setAttribute("payment-type", value.getPaymentType());
        writer.setAttribute("payment-method", value.getPaymentMethod());
        writer.setAttribute("status", value.getStatus());
        writer.setAttribute("substatus", value.getSubstatus());
        writer.setAttribute("creation-date", value.getCreationDate(), checkoutDateFormat.createLongDateFormat());
        writer.setAttribute("items-total", value.getItemsTotal());
        writer.setAttribute("total", value.getTotal());

        if(value.getItems() != null) {
            writer.startNode("items");
            for(OrderItem item : value.getItems()) {
                orderItemXmlSerializer.serializeXml(item, writer);
            }
            writer.endNode();
        }
        if(value.getDeliveryWithRegion() != null) {
            deliveryWithRegionXmlSerializer.serializeXml(value.getDeliveryWithRegion(), writer);
        }

        final Buyer buyer = value.getBuyer();
        if(buyer != null) {
            writer.startNode("buyer");
            writer.setAttribute("id", buyer.getId());
            writer.setAttribute("first-name", buyer.getFirstName());
            writer.setAttribute("last-name", buyer.getLastName());
            writer.setAttribute("middle-name", buyer.getMiddleName());
            writer.setAttribute("phone", buyer.getPhone());
            writer.setAttribute("email", buyer.getEmail());
            writer.endNode();
        }

        if(value.getNotes() != null) {
            writer.startNode("notes");
            writer.addValue(value.getNotes());
            writer.endNode();
        }

        writer.endNode();
    }
}
