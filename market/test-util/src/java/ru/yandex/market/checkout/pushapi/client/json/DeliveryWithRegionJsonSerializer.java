package ru.yandex.market.checkout.pushapi.client.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.common.json.JsonSerializer;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.json.order.AddressJsonSerializer;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;

import java.io.IOException;

@Component
public class DeliveryWithRegionJsonSerializer implements JsonSerializer<DeliveryWithRegion> {
    
    private AddressJsonSerializer addressJsonSerializer;
    private CheckoutDateFormat checkoutDateFormat;

    @Autowired
    public void setAddressJsonSerializer(AddressJsonSerializer addressJsonSerializer) {
        this.addressJsonSerializer = addressJsonSerializer;
    }

    @Autowired
    public void setCheckoutDateFormat(CheckoutDateFormat checkoutDateFormat) {
        this.checkoutDateFormat = checkoutDateFormat;
    }

    @Override
    public void serialize(DeliveryWithRegion value, JsonWriter writer) throws IOException {
        writer.startObject();

        writer.setAttribute("type", value.getType());
        writer.setAttribute("price", value.getPrice());
        writer.setAttribute("serviceName", value.getServiceName());
        writer.setAttribute("id", value.getId());

        final DeliveryDates deliveryDates = value.getDeliveryDates();
        if(deliveryDates != null) {
            writer.startObject("dates");
            writer.setAttribute("fromDate", deliveryDates.getFromDate());
            writer.setAttribute("toDate", deliveryDates.getToDate());
            writer.endObject();
        }

        final Region region = value.getRegion();
        if(region != null) {
            writer.startObject("region");
            writer.setAttribute("id", region.getId());
            writer.setAttribute("name", region.getName());
            writer.setAttribute("type", region.getRegionType());
            serializeParent(region.getParent(), writer);
            writer.endObject();
        }
        if(value.getAddress() != null) {
            writer.setAttribute("address");
            addressJsonSerializer.serialize(value.getAddress(), writer);
        }
        
        if(value.getOutletId() != null) {
            writer.startObject("outlet");
            writer.setAttribute("id", value.getOutletId());
            writer.endObject();
        }
        
        writer.endObject();
    }

    private void serializeParent(Region parent, JsonWriter writer) throws IOException {
        if(parent != null) {
            writer.startObject("parent");
            writer.setAttribute("id", parent.getId());
            writer.setAttribute("name", parent.getName());
            writer.setAttribute("type", parent.getRegionType());
            serializeParent(parent.getParent(), writer);
            writer.endObject();
        }
    }
}
