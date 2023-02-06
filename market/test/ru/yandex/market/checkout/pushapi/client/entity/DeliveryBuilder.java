package ru.yandex.market.checkout.pushapi.client.entity;

import java.math.BigDecimal;
import java.util.Set;

import ru.yandex.market.checkout.checkouter.delivery.AddressBuilder;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.VatType;

/**
 * @author msavelyev
 */
public class DeliveryBuilder extends BaseBuilder<DeliveryResponse, DeliveryBuilder> {

    public DeliveryBuilder() {
        super(new DeliveryResponse());
        object.setDeliveryDates(new DeliveryDatesBuilder().build());
        object.setServiceName("Pochta Rossii");
        object.setType(DeliveryType.DELIVERY);
        object.setPrice(new BigDecimal(1234));
    }

    public DeliveryBuilder setId(String id) {
        return withField("id", id);
    }

    public DeliveryBuilder setType(DeliveryType deliveryType) {
        return withField("type", deliveryType);
    }

    public DeliveryBuilder setPrice(BigDecimal price) {
        return withField("price", price);
    }

    public DeliveryBuilder setServiceName(String serviceName) {
        return withField("serviceName", serviceName);
    }

    public DeliveryBuilder setOutletIds(Set<Long> outletIds) {
        return withField("outletIds", outletIds);
    }

    public DeliveryBuilder setDeliveryDates(DeliveryDates deliveryDates) {
        return withField("deliveryDates", deliveryDates);
    }

    public DeliveryBuilder setDeliveryDates(DeliveryDatesBuilder deliveryDatesBuilder) {
        return withField("deliveryDates", deliveryDatesBuilder);
    }

    public DeliveryBuilder setRegionId(Long regionId) {
        return withField("regionId", regionId);
    }

    public DeliveryBuilder setAddress(AddressBuilder address) {
        return withField("address", address);
    }

    public DeliveryBuilder setShopAddress(AddressBuilder address) {
        return withField("shopAddress", address);
    }

    public DeliveryBuilder setShopDeliveryId(String shopDeliveryId) {
        return withField("shopDeliveryId", shopDeliveryId);
    }

    public DeliveryBuilder setVat(VatType vat) {
        return withField("vat", vat);
    }
}
