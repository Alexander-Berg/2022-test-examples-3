package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.BaseBean;
import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.CheckoutUserGroup;
import ru.yandex.autotests.market.checkouter.beans.Color;
import ru.yandex.autotests.market.checkouter.beans.Currency;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.SubStatus;
import ru.yandex.autotests.market.checkouter.beans.TaxSystem;
import ru.yandex.autotests.market.checkouter.beans.common.CoinInfo;
import ru.yandex.autotests.market.checkouter.beans.common.OrderPromo;
import ru.yandex.autotests.market.checkouter.beans.orders.certificates.Certificate;
import ru.yandex.autotests.market.checkouter.beans.testdata.builders.TestDataOrderBuilder;

import java.util.List;

/**
 * Created by belmatter on 13.10.14.
 */
public class TestDataOrder extends BaseBean {

    private Long id;
    private String shopOrderId;
    private Long shopId;
    private Long supplierId;
    private Boolean global;
    private Long shopJurId;
    private Status status;
    private SubStatus substatus; // согласно документации именно substatus
    private String creationDate;
    private Long creationDateTimestamp;
    private String updateDate;
    private Long updateDateTimestamp;
    private String statusUpdateDate;
    private Long statusUpdateDateTimestamp;
    private String statusExpiryDate;
    private Long statusExpiryDateTimestamp;
    private Currency currency;
    private String buyerCurrency;
    private Double itemsTotal;
    private Double buyerItemsTotal;
    private Double total;
    private Double buyerTotal;
    private Double buyerTotalBeforeDiscount;
    private Double buyerItemsTotalBeforeDiscount;
    private Double buyerItemsTotalDiscount;
    private Double buyerTotalDiscount;
    private Double feeTotal;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private Long paymentId;
    private Boolean fake;
    private CheckoutUserGroup userGroup;
    private List<TestDataItem> items;
    private TestDataDelivery delivery;
    private Buyer buyer;
    private String notes;
    private Integer[] cancelReasons;
    private Boolean noAuth;
    private String acceptMethod;
    private Boolean isBooked;
    private String balanceOrderId;
    private String displayOrderId;
    private TaxSystem taxSystem;
    private Color rgb;
    private List<OrderPromo> promos;
    private List<Long> coinIdsToUse;
    private CoinInfo coinInfo;
    private Certificate certificate;
    private Boolean fulfilment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShopOrderId() {
        return shopOrderId;
    }

    public void setShopOrderId(String shopOrderId) {
        this.shopOrderId = shopOrderId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }


    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    public Long getShopJurId() {
        return shopJurId;
    }

    public void setShopJurId(Long shopJurId) {
        this.shopJurId = shopJurId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<OrderPromo> getPromos() {
        return promos;
    }

    public void setPromos(List<OrderPromo> promos) {
        this.promos = promos;
    }

    public List<Long> getCoinIdsToUse() {
        return coinIdsToUse;
    }

    public void setCoinIdsToUse(List<Long> coinIdsToUse) {
        this.coinIdsToUse = coinIdsToUse;
    }

    public Double getBuyerTotalBeforeDiscount() {
        return buyerTotalBeforeDiscount;
    }

    public void setBuyerTotalBeforeDiscount(Double buyerTotalBeforeDiscount) {
        this.buyerTotalBeforeDiscount = buyerTotalBeforeDiscount;
    }

    public Double getBuyerItemsTotalBeforeDiscount() {
        return buyerItemsTotalBeforeDiscount;
    }

    public void setBuyerItemsTotalBeforeDiscount(Double buyerItemsTotalBeforeDiscount) {
        this.buyerItemsTotalBeforeDiscount = buyerItemsTotalBeforeDiscount;
    }

    public Double getBuyerItemsTotalDiscount() {
        return buyerItemsTotalDiscount;
    }

    public void setBuyerItemsTotalDiscount(Double buyerItemsTotalDiscount) {
        this.buyerItemsTotalDiscount = buyerItemsTotalDiscount;
    }

    public Double getBuyerTotalDiscount() {
        return buyerTotalDiscount;
    }

    public void setBuyerTotalDiscount(Double buyerTotalDiscount) {
        this.buyerTotalDiscount = buyerTotalDiscount;
    }

    public CoinInfo getCoinInfo() {
        return coinInfo;
    }

    public void setCoinInfo(CoinInfo coinInfo) {
        this.coinInfo = coinInfo;
    }

    /**
     * поддержка старого бина
     */
    public SubStatus getSubStatus() {
        return substatus;
    }

    /**
     * поддержка старого бина
     */
    public void setSubStatus(SubStatus subStatus) {
        this.substatus = subStatus;
    }

    public SubStatus getSubstatus() {
        return substatus;
    }

    public void setSubstatus(SubStatus substatus) {
        this.substatus = substatus;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getStatusUpdateDate() {
        return statusUpdateDate;
    }

    public void setStatusUpdateDate(String statusUpdateDate) {
        this.statusUpdateDate = statusUpdateDate;
    }

    public String getStatusExpiryDate() {
        return statusExpiryDate;
    }

    public void setStatusExpiryDate(String statusExpiryDate) {
        this.statusExpiryDate = statusExpiryDate;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getBuyerCurrency() {
        return buyerCurrency;
    }

    public void setBuyerCurrency(String buyerCurrency) {
        this.buyerCurrency = buyerCurrency;
    }

    public Double getItemsTotal() {
        return itemsTotal;
    }

    public void setItemsTotal(Double itemsTotal) {
        this.itemsTotal = itemsTotal;
    }

    public Double getBuyerItemsTotal() {
        return buyerItemsTotal;
    }

    public void setBuyerItemsTotal(Double buyerItemsTotal) {
        this.buyerItemsTotal = buyerItemsTotal;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getBuyerTotal() {
        return buyerTotal;
    }

    public void setBuyerTotal(Double buyerTotal) {
        this.buyerTotal = buyerTotal;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getFake() {
        return fake;
    }

    public void setFake(Boolean fake) {
        this.fake = fake;
    }

    public List<TestDataItem> getItems() {
        return items;
    }

    public void setItems(List<TestDataItem> items) {
        this.items = items;
    }

    public TestDataDelivery getDelivery() {
        return delivery;
    }

    public void setDelivery(TestDataDelivery delivery) {
        this.delivery = delivery;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public TestDataOrderBuilder but() {
        return new TestDataOrderBuilder().copy(this);
    }

    public Double getFeeTotal() {
        return feeTotal;
    }

    public void setFeeTotal(Double feeTotal) {
        this.feeTotal = feeTotal;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public CheckoutUserGroup getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(CheckoutUserGroup userGroup) {
        this.userGroup = userGroup;
    }

    public Integer[] getCancelReasons() {
        return cancelReasons;
    }

    public void setCancelReasons(Integer[] cancelReasons) {
        this.cancelReasons = cancelReasons;
    }

    public Long getCreationDateTimestamp() {
        return creationDateTimestamp;
    }

    public void setCreationDateTimestamp(Long creationDateTimestamp) {
        this.creationDateTimestamp = creationDateTimestamp;
    }

    public Long getUpdateDateTimestamp() {
        return updateDateTimestamp;
    }

    public void setUpdateDateTimestamp(Long updateDateTimestamp) {
        this.updateDateTimestamp = updateDateTimestamp;
    }

    public Long getStatusUpdateDateTimestamp() {
        return statusUpdateDateTimestamp;
    }

    public void setStatusUpdateDateTimestamp(Long statusUpdateDateTimestamp) {
        this.statusUpdateDateTimestamp = statusUpdateDateTimestamp;
    }

    public Long getStatusExpiryDateTimestamp() {
        return statusExpiryDateTimestamp;
    }

    public void setStatusExpiryDateTimestamp(Long statusExpiryDateTimestamp) {
        this.statusExpiryDateTimestamp = statusExpiryDateTimestamp;
    }

    public Boolean getNoAuth() {
        return noAuth;
    }

    public void setNoAuth(Boolean noAuth) {
        this.noAuth = noAuth;
    }

    public String getAcceptMethod() {
        return acceptMethod;
    }

    public void setAcceptMethod(String acceptMethod) {
        this.acceptMethod = acceptMethod;
    }

    public Boolean getBooked() {
        return isBooked;
    }

    public void setBooked(Boolean booked) {
        isBooked = booked;
    }

    public String getBalanceOrderId() {
        return balanceOrderId;
    }

    public void setBalanceOrderId(String balanceOrderId) {
        this.balanceOrderId = balanceOrderId;
    }

    public String getDisplayOrderId() {
        return displayOrderId;
    }

    public void setDisplayOrderId(String displayOrderId) {
        this.displayOrderId = displayOrderId;
    }

    public TaxSystem getTaxSystem() {
        return taxSystem;
    }

    public void setTaxSystem(TaxSystem taxSystem) {
        this.taxSystem = taxSystem;
    }

    public Color getRgb() {
        return rgb;
    }

    public void setRgb(Color rgb) {
        this.rgb = rgb;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Boolean getFulfilment() {
        return fulfilment;
    }

    public void setFulfilment(Boolean fulfilment) {
        this.fulfilment = fulfilment;
    }
}
