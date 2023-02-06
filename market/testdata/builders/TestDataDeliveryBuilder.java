package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.DeliveryPartnerType;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.VatType;
import ru.yandex.autotests.market.checkouter.beans.common.Address;
import ru.yandex.autotests.market.checkouter.beans.common.Outlet;
import ru.yandex.autotests.market.checkouter.beans.testdata.DeliveryDates;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataDelivery;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataShipment;
import ru.yandex.autotests.market.place.beans.common.DeliveryType;

import java.util.Arrays;
import java.util.List;

public class TestDataDeliveryBuilder implements Cloneable {
    protected TestDataDeliveryBuilder self;

    protected String id;
    private boolean isSetId;

    protected String hash;
    private boolean isSetHash;

    protected String type;
    private boolean isSetType;

    protected String serviceName;
    private boolean isSetServiceName;

    protected Double price;
    private boolean isSetPrice;

    protected Double buyerPrice;
    private boolean isSetBuyerPrice;

    protected Integer regionId;
    private boolean isSetRegionId;

    protected Address address;
    private boolean isSetAddress;

    protected DeliveryDates dates;
    private boolean isSetDates;

    protected Long outletId;
    private boolean isSetOutletId;

    protected List<Outlet> outlets;
    private boolean isSetOutlets;

    protected List<PaymentMethod> paymentMethods;
    private boolean isSetPaymentMethods;

    protected Boolean paymentAllow;
    private boolean isSetPaymentAllow;

    protected Address buyerAddress;
    private boolean isSetBuyerAddress;

    protected Address shopAddress;
    private boolean isSetShopAddress;

    protected VatType vat;
    private boolean isSetVat;

    protected DeliveryPartnerType deliveryPartnerType;
    private boolean isSetDeliveryPartnerType;

    protected Long deliveryServiceId;
    private boolean isSetDeliveryServiceId;

    protected TestDataShipment shipment;
    private boolean isSetShipment;

    public TestDataDeliveryBuilder() {
        self = this;
    }

    public TestDataDeliveryBuilder withId(String value) {
        this.id = value;
        this.isSetId = true;
        return self;
    }

    public TestDataDeliveryBuilder withHash(String value) {
        this.hash = value;
        this.isSetHash = true;
        return self;
    }

    public TestDataDeliveryBuilder withType(String value) {
        this.type = value;
        this.isSetType = true;
        return self;
    }

    public TestDataDeliveryBuilder withServiceName(String value) {
        this.serviceName = value;
        this.isSetServiceName = true;
        return self;
    }

    public TestDataDeliveryBuilder withPrice(Double value) {
        this.price = value;
        this.isSetPrice = true;
        return self;
    }

    public TestDataDeliveryBuilder withBuyerPrice(Double value) {
        this.buyerPrice = value;
        this.isSetBuyerPrice = true;
        return self;
    }

    public TestDataDeliveryBuilder withRegionId(Integer value) {
        this.regionId = value;
        this.isSetRegionId = true;
        return self;
    }

    public TestDataDeliveryBuilder withAddress(Address value) {
        this.address = value;
        this.isSetAddress = true;
        return self;
    }

    public TestDataDeliveryBuilder withDates(DeliveryDates value) {
        this.dates = value;
        this.isSetDates = true;
        return self;
    }

    public TestDataDeliveryBuilder withOutletId(Long value) {
        this.outletId = value;
        this.isSetOutletId = true;
        return self;
    }

    public TestDataDeliveryBuilder withOutlets(List<Outlet> value) {
        this.outlets = value;
        this.isSetOutlets = true;
        return self;
    }

    public TestDataDeliveryBuilder withPaymentMethods(List<PaymentMethod> value) {
        this.paymentMethods = value;
        this.isSetPaymentMethods = true;
        return self;
    }

    public TestDataDeliveryBuilder withPaymentAllow(Boolean value) {
        this.paymentAllow = value;
        this.isSetPaymentAllow = true;
        return self;
    }

    public TestDataDeliveryBuilder withBuyerAddress(Address value) {
        this.buyerAddress = value;
        this.isSetBuyerAddress = true;
        return self;
    }

    public TestDataDeliveryBuilder withShopAddress(Address value) {
        this.shopAddress = value;
        this.isSetShopAddress = true;
        return self;
    }

    public TestDataDeliveryBuilder withVat(VatType vat) {
        this.vat = vat;
        this.isSetVat = true;
        return self;
    }

    public TestDataDeliveryBuilder withDeliveryPartnerType(DeliveryPartnerType value) {
        this.deliveryPartnerType = value;
        this.isSetDeliveryPartnerType = true;
        return self;
    }

    public TestDataDeliveryBuilder withDeliveryServiceId(Long value) {
        this.deliveryServiceId = value;
        this.isSetDeliveryServiceId = true;
        return self;
    }

    public TestDataDeliveryBuilder withShipment(TestDataShipment value) {
        this.shipment = value;
        this.isSetShipment = true;
        return self;
    }

    @Override
    public Object clone() {
        try {
            TestDataDeliveryBuilder result = (TestDataDeliveryBuilder)super.clone();
            result.self = result;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    public TestDataDeliveryBuilder but() {
        return (TestDataDeliveryBuilder)clone();
    }

    public TestDataDeliveryBuilder copy(TestDataDelivery delivery) {
        withId(delivery.getId());
        withHash(delivery.getHash());
        withType(delivery.getType());
        withServiceName(delivery.getServiceName());
        withPrice(delivery.getPrice());
        withBuyerPrice(delivery.getBuyerPrice());
        withRegionId(delivery.getRegionId());
        withAddress(delivery.getAddress());
        withDates(delivery.getDates());
        withOutletId(delivery.getOutletId());
        withOutlets(delivery.getOutlets());
        withPaymentMethods(delivery.getPaymentMethods());
        withPaymentAllow(delivery.getPaymentAllow());
        withBuyerAddress(delivery.getBuyerAddress());
        withShopAddress(delivery.getShopAddress());
        withVat(delivery.getVat());
        withDeliveryPartnerType(delivery.getDeliveryPartnerType());
        withDeliveryServiceId(delivery.getDeliveryServiceId());
        withShipment(delivery.getShipment());
        return self;
    }

    public TestDataDelivery build() {
        try {
            TestDataDelivery result = new TestDataDelivery();
            if (isSetId) {
                result.setId(id);
            }
            if (isSetHash) {
                result.setHash(hash);
            }
            if (isSetType) {
                result.setType(type);
            }
            if (isSetServiceName) {
                result.setServiceName(serviceName);
            }
            if (isSetPrice) {
                result.setPrice(price);
            }
            if (isSetBuyerPrice) {
                result.setBuyerPrice(buyerPrice);
            }
            if (isSetRegionId) {
                result.setRegionId(regionId);
            }
            if (isSetAddress) {
                result.setAddress(address);
            }
            if (isSetDates) {
                result.setDates(dates);
            }
            if (isSetOutletId) {
                result.setOutletId(outletId);
            }
            if (isSetOutlets) {
                result.setOutlets(outlets);
            }
            if (isSetPaymentMethods) {
                result.setPaymentMethods(paymentMethods);
            }
            if (isSetPaymentAllow) {
                result.setPaymentAllow(paymentAllow);
            }
            if (isSetBuyerAddress) {
                result.setBuyerAddress(buyerAddress);
            }
            if (isSetShopAddress) {
                result.setShopAddress(shopAddress);
            }
            if (isSetVat) {
                result.setVat(vat);
            }
            if (isSetDeliveryPartnerType) {
                result.setDeliveryPartnerType(deliveryPartnerType);
            }
            if (isSetDeliveryServiceId) {
                result.setDeliveryServiceId(deliveryServiceId);
            }
            if (isSetShipment) {
                result.setShipment(shipment);
            }
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

    public TestDataDeliveryBuilder withType(DeliveryType deliveryType){
        return withType(deliveryType.name());
    }

    public TestDataDeliveryBuilder withOutlets(Outlet... outlets){
        return this.withOutlets(Arrays.asList(outlets));
    }

    public TestDataDeliveryBuilder withPaymentMethods(PaymentMethod... paymentMethods){
        return this.withPaymentMethods(Arrays.asList(paymentMethods));
    }
}
