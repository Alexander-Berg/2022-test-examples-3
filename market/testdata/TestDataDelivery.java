package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.BaseBean;
import ru.yandex.autotests.market.checkouter.beans.DeliveryPartnerType;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.VatType;
import ru.yandex.autotests.market.checkouter.beans.common.Address;
import ru.yandex.autotests.market.checkouter.beans.common.ItemPromo;
import ru.yandex.autotests.market.checkouter.beans.common.Outlet;
import ru.yandex.autotests.market.checkouter.beans.testdata.builders.TestDataDeliveryBuilder;

import java.util.List;

/**
 * Created by belmatter on 10.10.14.
 */
public class TestDataDelivery extends BaseBean {

    private String id;

    private String hash;

    private String type;

    private String serviceName;

    private Double price;

    private Double buyerPrice;

    private Double buyerPriceBeforeDiscount;

    private Double buyerDiscount;

    private List<ItemPromo> promos;

    private Integer regionId;

    private Address address;

    private Address buyerAddress;

    private Address shopAddress;

    private Long outletId;

    private DeliveryDates dates;

    private List<Outlet> outlets;

    private List<PaymentMethod> paymentMethods;

    private Boolean paymentAllow;

    private List<TestDataTrack> tracks;

    private VatType vat;

    private String balanceOrderId;

    private DeliveryPartnerType deliveryPartnerType;

    private Long deliveryServiceId;

    private TestDataShipment shipment;

    private List<TestDataShipment> parcels;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getBuyerPrice() {
        return buyerPrice;
    }

    public void setBuyerPrice(Double buyerPrice) {
        this.buyerPrice = buyerPrice;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public DeliveryDates getDates() {
        return dates;
    }

    public void setDates(DeliveryDates dates) {
        this.dates = dates;
    }

    public Long getOutletId() {
        return outletId;
    }

    public void setOutletId(Long outletId) {
        this.outletId = outletId;
    }

    public List<Outlet> getOutlets() {
        return outlets;
    }

    public void setOutlets(List<Outlet> outlets) {
        this.outlets = outlets;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public Boolean getPaymentAllow() {
        return paymentAllow;
    }

    public void setPaymentAllow(Boolean paymentAllow) {
        this.paymentAllow = paymentAllow;
    }

    public TestDataDeliveryBuilder but() {
        return new TestDataDeliveryBuilder().copy(this);
    }

    public Address getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(Address buyerAddress) {
        this.buyerAddress = buyerAddress;
    }

    public Address getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(Address shopAddress) {
        this.shopAddress = shopAddress;
    }

    public List<TestDataTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<TestDataTrack> tracks) {
        this.tracks = tracks;
    }

    public VatType getVat() {
        return vat;
    }

    public void setVat(VatType vat) {
        this.vat = vat;
    }

    public String getBalanceOrderId() {
        return balanceOrderId;
    }

    public void setBalanceOrderId(String balanceOrderId) {
        this.balanceOrderId = balanceOrderId;
    }

    public DeliveryPartnerType getDeliveryPartnerType() {
        return deliveryPartnerType;
    }

    public void setDeliveryPartnerType(DeliveryPartnerType deliveryPartnerType) {
        this.deliveryPartnerType = deliveryPartnerType;
    }

    public Long getDeliveryServiceId() {
        return deliveryServiceId;
    }

    public void setDeliveryServiceId(Long deliveryServiceId) {
        this.deliveryServiceId = deliveryServiceId;
    }

    public TestDataShipment getShipment() {
        return shipment;
    }

    public void setShipment(TestDataShipment shipment) {
        this.shipment = shipment;
    }

    public Double getBuyerPriceBeforeDiscount() {
        return buyerPriceBeforeDiscount;
    }

    public void setBuyerPriceBeforeDiscount(Double buyerPriceBeforeDiscount) {
        this.buyerPriceBeforeDiscount = buyerPriceBeforeDiscount;
    }

    public List<ItemPromo> getPromos() {
        return promos;
    }

    public void setPromos(List<ItemPromo> promos) {
        this.promos = promos;
    }

    public Double getBuyerDiscount() {
        return buyerDiscount;
    }

    public void setBuyerDiscount(Double buyerDiscount) {
        this.buyerDiscount = buyerDiscount;
    }

    public List<TestDataShipment> getParcels() {
        return parcels;
    }

    public void setParcels(List<TestDataShipment> parcels) {
        this.parcels = parcels;
    }
}
