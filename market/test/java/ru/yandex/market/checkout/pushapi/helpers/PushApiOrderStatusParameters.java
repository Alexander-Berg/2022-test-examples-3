package ru.yandex.market.checkout.pushapi.helpers;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.PushApiDelivery;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

public class PushApiOrderStatusParameters {
    public static final long DEFAULT_SHOP_ID = 242104L;
    public static final boolean DEFAULT_SANDBOX = false;
    public static final boolean DEFAULT_PARTNER_INTERFACE = false;
    public static final DataType DEFAULT_DATA_TYPE = DataType.XML;

    private final PushApiOrder orderChange;
    private long shopId = DEFAULT_SHOP_ID;
    private boolean sandbox = DEFAULT_SANDBOX;
    private boolean partnerInterface = DEFAULT_PARTNER_INTERFACE;
    private DataType dataType = DEFAULT_DATA_TYPE;

    public PushApiOrderStatusParameters() {
        this(OrderProvider.getBlueOrder(o -> {
            o.setAcceptMethod(OrderAcceptMethod.PUSH_API);
            o.setDelivery(DeliveryProvider.shopSelfDelivery().build());

            Address shopAddress = AddressProvider.getAddress();
            shopAddress.setType(AddressType.SHOP);

            o.setId(1234L);
            o.setStatus(OrderStatus.PROCESSING);
            o.getDelivery().setShopAddress(shopAddress);
            o.setNotes("notes");
        }));
    }

    public PushApiOrderStatusParameters(Order orderChange) {
        this.orderChange = new PushApiOrder(orderChange);
        PushApiDelivery delivery = this.orderChange.getDelivery();
        Courier courier = new Courier();
        courier.setVehicleNumber("а123бв 71 RUS");
        courier.setVehicleDescription("Фиолетовая KIA Rio");
        delivery.setCourier(courier);
        this.orderChange.setDelivery(delivery);
    }

    public PushApiOrder getOrderChange() {
        return orderChange;
    }

    public long getShopId() {
        return shopId;
    }

    public void setShopId(long shopId) {
        this.shopId = shopId;
    }

    public void setEacc(String eacc){
        orderChange.setElectronicAcceptanceCertificateCode(eacc);
    }

    @Override
    public String toString() {
        return "PushApiOrderStatusParameters{" +
                "orderChange=" + orderChange +
                ", shopId=" + shopId +
                '}';
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

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
}
