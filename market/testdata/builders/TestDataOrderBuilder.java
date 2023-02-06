package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import org.joda.time.DateTime;
import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.CheckoutUserGroup;
import ru.yandex.autotests.market.checkouter.beans.Color;
import ru.yandex.autotests.market.checkouter.beans.Currency;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.SubStatus;
import ru.yandex.autotests.market.checkouter.beans.TaxSystem;
import ru.yandex.autotests.market.checkouter.beans.orders.certificates.Certificate;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataDelivery;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;

import java.util.Arrays;
import java.util.List;


public class TestDataOrderBuilder implements Cloneable {
    private static final String DATE_FORMAT = "dd-MM-YYYY HH:mm:ss";

    protected TestDataOrderBuilder self;

    protected Long id;
    private boolean isSetId;

    protected String shopOrderId;
    private boolean isSetShopOrderId;

    protected Long shopId;
    private boolean isSetShopId;

    protected Long supplierId;
    private boolean isSetSupplierId;

    protected Boolean global;
    private boolean isSetGlobal;

    protected Long shopJurId;
    private boolean isSetShopJurId;

    protected Status status;
    private boolean isSetStatus;

    protected SubStatus substatus;
    private boolean isSetSubstatus;

    protected String creationDate;
    private boolean isSetCreationDate;

    protected String updateDate;
    private boolean isSetUpdateDate;

    protected String statusUpdateDate;
    private boolean isSetStatusUpdateDate;

    protected String statusExpiryDate;
    private boolean isSetStatusExpiryDate;

    protected Currency currency;
    private boolean isSetCurrency;

    protected String buyerCurrency;
    private boolean isSetBuyerCurrency;

    protected Double itemsTotal;
    private boolean isSetItemsTotal;

    protected Double buyerItemsTotal;
    private boolean isSetBuyerItemsTotal;

    protected Double total;
    private boolean isSetTotal;

    protected Double buyerTotal;
    private boolean isSetBuyerTotal;

    protected PaymentType paymentType;
    private boolean isSetPaymentType;

    protected PaymentMethod paymentMethod;
    private boolean isSetPaymentMethod;

    protected Boolean fake;
    private boolean isSetFake;

    protected List<TestDataItem> items;
    private boolean isSetItems;

    protected TestDataDelivery delivery;
    private boolean isSetDelivery;

    protected Buyer buyer;
    private boolean isSetBuyer;

    protected String notes;
    private boolean isSetNotes;

    protected Double feeTotal;
    private boolean isSetFeeTotal;

    protected Long paymentId;
    private boolean isSetPaymentId;

    protected CheckoutUserGroup userGroup;
    private boolean isSetUserGroup;

    protected Integer[] cancelReasons;
    private boolean isSetCancelReasons;

    protected Long creationDateTimestamp;
    private boolean isSetCreationDateTimestamp;

    protected Long updateDateTimestamp;
    private boolean isSetUpdateDateTimestamp;

    protected Long statusUpdateDateTimestamp;
    private boolean isSetStatusUpdateDateTimestamp;

    protected Long statusExpiryDateTimestamp;
    private boolean isSetStatusExpiryDateTimestamp;

    protected Boolean noAuth;
    private boolean isSetNoAuth;

    protected String acceptMethod;
    private boolean isSetAcceptMethod;

    protected Boolean booked;
    private boolean isSetBooked;

    protected String balanceOrderId;
    private boolean isSetBalanceOrderId;

    protected String displayOrderId;
    private boolean isSetDisplayOrderId;

    private TaxSystem taxSystem;
    private boolean isSetTaxSystem;

    private Certificate certificate;
    private boolean isSetCertificate;

    private List<Long> coinIdsToUse;
    private Color rgb;

    public TestDataOrderBuilder() {
        self = this;
    }

    public TestDataOrderBuilder withId(Long value) {
        this.id = value;
        this.isSetId = true;
        return self;
    }

    public TestDataOrderBuilder withShopOrderId(String value) {
        this.shopOrderId = value;
        this.isSetShopOrderId = true;
        return self;
    }

    public TestDataOrderBuilder withShopId(Long value) {
        this.shopId = value;
        this.isSetShopId = true;
        return self;
    }

    public TestDataOrderBuilder withSupplierId(Long value) {
        this.supplierId = value;
        this.isSetSupplierId = true;
        return self;
    }

    public TestDataOrderBuilder withGlobal(Boolean value) {
        this.global = value;
        this.isSetGlobal = true;
        return self;
    }

    public TestDataOrderBuilder withShopJurId(Long value) {
        this.shopJurId = value;
        this.isSetShopJurId = true;
        return self;
    }

    public TestDataOrderBuilder withStatus(Status value) {
        this.status = value;
        this.isSetStatus = true;
        return self;
    }

    public TestDataOrderBuilder withSubStatus(SubStatus value) {
        this.substatus = value;
        this.isSetSubstatus = true;
        return self;
    }

    public TestDataOrderBuilder withCreationDate(String value) {
        this.creationDate = value;
        this.isSetCreationDate = true;
        return self;
    }

    public TestDataOrderBuilder withUpdateDate(String value) {
        this.updateDate = value;
        this.isSetUpdateDate = true;
        return self;
    }

    public TestDataOrderBuilder withStatusUpdateDate(String value) {
        this.statusUpdateDate = value;
        this.isSetStatusUpdateDate = true;
        return self;
    }

    public TestDataOrderBuilder withStatusExpiryDate(String value) {
        this.statusExpiryDate = value;
        this.isSetStatusExpiryDate = true;
        return self;
    }

    public TestDataOrderBuilder withCurrency(Currency value) {
        this.currency = value;
        this.isSetCurrency = true;
        return self;
    }

    public TestDataOrderBuilder withBuyerCurrency(String value) {
        this.buyerCurrency = value;
        this.isSetBuyerCurrency = true;
        return self;
    }

    public TestDataOrderBuilder withItemsTotal(Double value) {
        this.itemsTotal = value;
        this.isSetItemsTotal = true;
        return self;
    }

    public TestDataOrderBuilder withBuyerItemsTotal(Double value) {
        this.buyerItemsTotal = value;
        this.isSetBuyerItemsTotal = true;
        return self;
    }

    public TestDataOrderBuilder withTotal(Double value) {
        this.total = value;
        this.isSetTotal = true;
        return self;
    }

    public TestDataOrderBuilder withBuyerTotal(Double value) {
        this.buyerTotal = value;
        this.isSetBuyerTotal = true;
        return self;
    }

    public TestDataOrderBuilder withPaymentType(PaymentType value) {
        this.paymentType = value;
        this.isSetPaymentType = true;
        return self;
    }

    public TestDataOrderBuilder withPaymentMethod(PaymentMethod value) {
        this.paymentMethod = value;
        this.isSetPaymentMethod = true;
        return self;
    }

    public TestDataOrderBuilder withFake(Boolean value) {
        this.fake = value;
        this.isSetFake = true;
        return self;
    }

    public TestDataOrderBuilder withItems(List<TestDataItem> value) {
        this.items = value;
        this.isSetItems = true;
        return self;
    }

    public TestDataOrderBuilder withDelivery(TestDataDelivery value) {
        this.delivery = value;
        this.isSetDelivery = true;
        return self;
    }

    public TestDataOrderBuilder withBuyer(Buyer value) {
        this.buyer = value;
        this.isSetBuyer = true;
        return self;
    }

    public TestDataOrderBuilder withNotes(String value) {
        this.notes = value;
        this.isSetNotes = true;
        return self;
    }

    public TestDataOrderBuilder withFeeTotal(Double value) {
        this.feeTotal = value;
        this.isSetFeeTotal = true;
        return self;
    }

    public TestDataOrderBuilder withPaymentId(Long value) {
        this.paymentId = value;
        this.isSetPaymentId = true;
        return self;
    }

    public TestDataOrderBuilder withUserGroup(CheckoutUserGroup value) {
        this.userGroup = value;
        this.isSetUserGroup = true;
        return self;
    }

    public TestDataOrderBuilder withCancelReasons(Integer[] value) {
        this.cancelReasons = value;
        this.isSetCancelReasons = true;
        return self;
    }

    public TestDataOrderBuilder withCreationDateTimestamp(Long value) {
        this.creationDateTimestamp = value;
        this.isSetCreationDateTimestamp = true;
        return self;
    }

    public TestDataOrderBuilder withUpdateDateTimestamp(Long value) {
        this.updateDateTimestamp = value;
        this.isSetUpdateDateTimestamp = true;
        return self;
    }

    public TestDataOrderBuilder withStatusUpdateDateTimestamp(Long value) {
        this.statusUpdateDateTimestamp = value;
        this.isSetStatusUpdateDateTimestamp = true;
        return self;
    }

    public TestDataOrderBuilder withStatusExpiryDateTimestamp(Long value) {
        this.statusExpiryDateTimestamp = value;
        this.isSetStatusExpiryDateTimestamp = true;
        return self;
    }

    public TestDataOrderBuilder withNoAuth(Boolean value) {
        this.noAuth = value;
        this.isSetNoAuth = true;
        return self;
    }

    public TestDataOrderBuilder withAcceptMethod(String value) {
        this.acceptMethod = value;
        this.isSetAcceptMethod = true;
        return self;
    }

    public TestDataOrderBuilder withBooked(Boolean value) {
        this.booked = value;
        this.isSetBooked = true;
        return self;
    }

    public TestDataOrderBuilder withBalanceOrderId(String value) {
        this.balanceOrderId = value;
        this.isSetBalanceOrderId = true;
        return self;
    }

    public TestDataOrderBuilder withDisplayOrderId(String value) {
        this.displayOrderId = value;
        this.isSetDisplayOrderId = true;
        return self;
    }

    public TestDataOrderBuilder withTaxSystem(TaxSystem value) {
        this.taxSystem = value;
        this.isSetTaxSystem = true;
        return self;
    }


    public TestDataOrderBuilder withRgb(Color rgb) {
        this.rgb = rgb;
        return self;
    }

    public TestDataOrderBuilder withCoinIdsToUse(List<Long> coinIdsToUse) {
        this.coinIdsToUse = coinIdsToUse;
        return self;
    }

    public TestDataOrderBuilder withCertificate(Certificate certificate) {
        this.certificate = certificate;
        this.isSetCertificate = true;
        return self;
    }

    @Override
    public Object clone() {
        try {
            TestDataOrderBuilder result = (TestDataOrderBuilder) super.clone();
            result.self = result;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    public TestDataOrderBuilder but() {
        return (TestDataOrderBuilder) clone();
    }

    public TestDataOrderBuilder copy(TestDataOrder order) {
        withId(order.getId());
        withShopOrderId(order.getShopOrderId());
        withShopId(order.getShopId());
        withSupplierId(order.getSupplierId());
        withGlobal(order.getGlobal());
        withShopJurId(order.getShopJurId());
        withStatus(order.getStatus());
        withSubStatus(order.getSubstatus());
        withCreationDate(order.getCreationDate());
        withUpdateDate(order.getUpdateDate());
        withStatusUpdateDate(order.getStatusUpdateDate());
        withStatusExpiryDate(order.getStatusExpiryDate());
        withCurrency(order.getCurrency());
        withBuyerCurrency(order.getBuyerCurrency());
        withItemsTotal(order.getItemsTotal());
        withBuyerItemsTotal(order.getBuyerItemsTotal());
        withTotal(order.getTotal());
        withBuyerTotal(order.getBuyerTotal());
        withPaymentType(order.getPaymentType());
        withPaymentMethod(order.getPaymentMethod());
        withFake(order.getFake());
        withItems(order.getItems());
        withDelivery(order.getDelivery());
        withBuyer(order.getBuyer());
        withNotes(order.getNotes());
        withFeeTotal(order.getFeeTotal());
        withPaymentId(order.getPaymentId());
        withUserGroup(order.getUserGroup());
        withCancelReasons(order.getCancelReasons());
        withCreationDateTimestamp(order.getCreationDateTimestamp());
        withUpdateDateTimestamp(order.getUpdateDateTimestamp());
        withStatusUpdateDateTimestamp(order.getStatusUpdateDateTimestamp());
        withStatusExpiryDateTimestamp(order.getStatusExpiryDateTimestamp());
        withNoAuth(order.getNoAuth());
        withAcceptMethod(order.getAcceptMethod());
        withBooked(order.getBooked());
        withBalanceOrderId(order.getBalanceOrderId());
        withDisplayOrderId(order.getDisplayOrderId());
        withTaxSystem(order.getTaxSystem());
        withRgb(order.getRgb());
        withCoinIdsToUse(order.getCoinIdsToUse());
        withCertificate(order.getCertificate());
        return self;
    }

    public TestDataOrder build() {
        try {
            TestDataOrder result = new TestDataOrder();
            if (isSetId) {
                result.setId(id);
            }
            if (isSetShopOrderId) {
                result.setShopOrderId(shopOrderId);
            }
            if (isSetShopId) {
                result.setShopId(shopId);
            }
            if (isSetSupplierId) {
                result.setSupplierId(supplierId);
            }
            if (isSetGlobal) {
                result.setGlobal(global);
            }
            if (isSetShopJurId) {
                result.setShopJurId(shopJurId);
            }
            if (isSetStatus) {
                result.setStatus(status);
            }
            if (isSetSubstatus) {
                result.setSubstatus(substatus);
            }
            if (isSetCreationDate) {
                result.setCreationDate(creationDate);
            }
            if (isSetUpdateDate) {
                result.setUpdateDate(updateDate);
            }
            if (isSetStatusUpdateDate) {
                result.setStatusUpdateDate(statusUpdateDate);
            }
            if (isSetStatusExpiryDate) {
                result.setStatusExpiryDate(statusExpiryDate);
            }
            if (isSetCurrency) {
                result.setCurrency(currency);
            }
            if (isSetBuyerCurrency) {
                result.setBuyerCurrency(buyerCurrency);
            }
            if (isSetItemsTotal) {
                result.setItemsTotal(itemsTotal);
            }
            if (isSetBuyerItemsTotal) {
                result.setBuyerItemsTotal(buyerItemsTotal);
            }
            if (isSetTotal) {
                result.setTotal(total);
            }
            if (isSetBuyerTotal) {
                result.setBuyerTotal(buyerTotal);
            }
            if (isSetPaymentType) {
                result.setPaymentType(paymentType);
            }
            if (isSetPaymentMethod) {
                result.setPaymentMethod(paymentMethod);
            }
            if (isSetFake) {
                result.setFake(fake);
            }
            if (isSetItems) {
                result.setItems(items);
            }
            if (isSetDelivery) {
                result.setDelivery(delivery);
            }
            if (isSetBuyer) {
                result.setBuyer(buyer);
            }
            if (isSetNotes) {
                result.setNotes(notes);
            }
            if (isSetFeeTotal) {
                result.setFeeTotal(feeTotal);
            }
            if (isSetPaymentId) {
                result.setPaymentId(paymentId);
            }
            if (isSetUserGroup) {
                result.setUserGroup(userGroup);
            }
            if (isSetCancelReasons) {
                result.setCancelReasons(cancelReasons);
            }
            if (isSetCreationDateTimestamp) {
                result.setCreationDateTimestamp(creationDateTimestamp);
            }
            if (isSetUpdateDateTimestamp) {
                result.setUpdateDateTimestamp(updateDateTimestamp);
            }
            if (isSetStatusUpdateDateTimestamp) {
                result.setStatusUpdateDateTimestamp(statusUpdateDateTimestamp);
            }
            if (isSetStatusExpiryDateTimestamp) {
                result.setStatusExpiryDateTimestamp(statusExpiryDateTimestamp);
            }
            if (isSetNoAuth) {
                result.setNoAuth(noAuth);
            }
            if (isSetAcceptMethod) {
                result.setAcceptMethod(acceptMethod);
            }
            if (isSetBooked) {
                result.setBooked(booked);
            }
            if (isSetBalanceOrderId) {
                result.setBalanceOrderId(balanceOrderId);
            }
            if (isSetDisplayOrderId) {
                result.setDisplayOrderId(displayOrderId);
            }
            if (isSetTaxSystem) {
                result.setTaxSystem(taxSystem);
            }
            if (isSetCertificate) {
                result.setCertificate(certificate);
            }
            if (rgb != null) {
                result.setRgb(rgb);
            }

            result.setCoinIdsToUse(coinIdsToUse);

            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

    public TestDataOrderBuilder withItems(TestDataItem... items) {
        return withItems(Arrays.asList(items));
    }

    public TestDataOrderBuilder withStatusUpdateDate(DateTime date) {
        return withStatusUpdateDate(date.toString(DATE_FORMAT));
    }
}
