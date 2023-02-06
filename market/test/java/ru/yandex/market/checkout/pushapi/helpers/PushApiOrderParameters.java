package ru.yandex.market.checkout.pushapi.helpers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

public class PushApiOrderParameters {

    public static final long DEFAULT_SHOP_ID = 242103L;
    public static final boolean SANDBOX = false;
    public static final boolean DEFAULT_PARTNER_INTERFACE = false;

    private final Order order;
    private OrderResponse orderResponse;
    private long shopId = DEFAULT_SHOP_ID;
    private boolean sandbox;
    private boolean partnerInterface = DEFAULT_PARTNER_INTERFACE;
    private DataType dataType = DataType.JSON;
    private Features features = Features.builder().build();

    public PushApiOrderParameters() {
        this(OrderProvider.getBlueOrder((o) -> {
            o.setAcceptMethod(OrderAcceptMethod.PUSH_API);
            o.setDelivery(DeliveryProvider.shopSelfDelivery().build());

            Address address = AddressProvider.getAddress();
            address.setType(AddressType.SHOP);
            o.setStatus(OrderStatus.PLACING);
            o.setId(1234L);
            o.getDelivery().setShopAddress(address);
            Parcel parcel = new Parcel();
            parcel.setId(70511219L);
            parcel.setShipmentDate(LocalDate.now());
            parcel.setShipmentTime(LocalDateTime.now());
            o.getDelivery().setParcels(List.of(parcel));
            o.getPromoPrices().setBuyerItemsTotalBeforeDiscount(BigDecimal.valueOf(350L));
            o.getPromoPrices().setBuyerTotalBeforeDiscount(BigDecimal.valueOf(360L));
            o.getItems().forEach(orderItem ->
                    orderItem.getPrices().setBuyerPriceBeforeDiscount(BigDecimal.valueOf(255L)));
        }));
        orderResponse = new OrderResponse(String.valueOf(order.getId()), true, null,
                Date.from(LocalDate.now().atStartOfDay(ZoneOffset.systemDefault()).toInstant()));
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    public PushApiOrderParameters(Order order) {
        this.order = order;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public Order getOrder() {
        return order;
    }

    public OrderResponse getOrderResponse() {
        return orderResponse;
    }

    public void setOrderResponse(OrderResponse orderResponse) {
        this.orderResponse = orderResponse;
    }

    public long getShopId() {
        return shopId;
    }

    public void setShopId(long shopId) {
        this.shopId = shopId;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public boolean isPartnerInterface() {
        return partnerInterface;
    }

    public void setPartnerInterface(boolean partnerInterface) {
        this.partnerInterface = partnerInterface;
    }

    @Override
    public String toString() {
        return "PushApiOrderParameters{" +
                "order=" + order +
                ", shopId=" + shopId +
                ", sandbox=" + sandbox +
                '}';
    }
}
