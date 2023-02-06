package ru.yandex.market.checkout.pushapi.client.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.AbstractAutowiringXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;

import java.io.IOException;
import java.util.Date;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeliveryWithRegionXmlSerializer extends AbstractAutowiringXmlSerializer<DeliveryWithRegion> {

    private CheckoutDateFormat checkoutDateFormat;
    private AddressXmlSerializer addressXmlSerializer;

    @Autowired
    public void setCheckoutDateFormat(CheckoutDateFormat checkoutDateFormat) {
        this.checkoutDateFormat = checkoutDateFormat;
    }

    @Autowired
    public void setAddressXmlSerializer(AddressXmlSerializer addressXmlSerializer) {
        this.addressXmlSerializer = addressXmlSerializer;
    }

    @Override
    public void serializeXml(DeliveryWithRegion value, PrimitiveXmlWriter writer) throws IOException {

        writer.startNode("delivery");
        
        writer.setAttribute("type", value.getType());
        writer.setAttribute("price", value.getPrice());
        writer.setAttribute("region-id", value.getRegionId());
        writer.setAttribute("service-name", value.getServiceName());
        writer.setAttribute("id", value.getId());
        final DeliveryDates deliveryDates = value.getDeliveryDates();
        if(deliveryDates != null) {
            writer.startNode("dates");
            final Date fromDate = deliveryDates.getFromDate();
            final Date toDate = deliveryDates.getToDate();
            if(fromDate != null) {
                writer.setAttribute("from-date", checkoutDateFormat.formatShort(fromDate));
            }
            if(toDate != null) {
                writer.setAttribute("to-date", checkoutDateFormat.formatShort(toDate));
            }
            writer.endNode();
        }

        final Region region = value.getRegion();
        if(region != null) {
            writer.startNode("region");
            writer.setAttribute("id", region.getId());
            writer.setAttribute("name", region.getName());
            writer.setAttribute("type", region.getRegionType());
            serializeParent(region.getParent(), writer);
            writer.endNode();
        }
        if(value.getAddress() != null) {
            addressXmlSerializer.serializeXml(value.getAddress(), writer);
        }
        
        if(value.getOutletId() != null) {
            writer.startNode("outlet");
            writer.setAttribute("id", value.getOutletId());
            writer.endNode();
        }
        
        writer.endNode();
    }

    private void serializeParent(Region parent, PrimitiveXmlWriter writer) throws IOException {
        if(parent != null) {
            writer.startNode("parent");
            writer.setAttribute("id", parent.getId());
            writer.setAttribute("name", parent.getName());
            writer.setAttribute("type", parent.getRegionType());
            serializeParent(parent.getParent(), writer);

            writer.endNode();
        }
    }
    
}
