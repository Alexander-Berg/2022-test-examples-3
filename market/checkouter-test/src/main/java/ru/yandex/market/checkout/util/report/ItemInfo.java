package ru.yandex.market.checkout.util.report;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.common.report.model.PayByYaPlus;
import ru.yandex.market.common.report.model.RawParam;
import ru.yandex.market.common.report.model.json.common.Region;

/**
 * @author Nikolai Iusiumbeli
 * date: 24/10/2017
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public class ItemInfo {

    public String promoType;
    public String promoKey;
    private boolean hideOffer = false;
    private final Prices prices = new Prices();
    private Fulfilment fulfilment = new Fulfilment();
    private BigDecimal weight;
    private List<String> dimensions;
    private boolean hideWeight = false;
    private boolean hideDimensions = false;
    private SupplierType supplierType;
    // Для кейзов, когда в /cart не передаем wareMd5
    private String wareMd5;
    private String feedGroupIdHash;
    private String picUrl;
    private List<RawParam> rawParams;

    private String supplierDescription;
    private List<Region> manufacturerCountries;
    private String supplierWorkSchedule;
    private String showInfo;
    private Boolean atSupplierWarehouse;
    private long externalFeedId;
    private Long fulfillmentWarehouseId;
    private Boolean downloadable;

    private PayByYaPlus payByYaPlus;

    public Prices getPrices() {
        return prices;
    }

    public boolean hideOffer() {
        return hideOffer;
    }

    public void setHideOffer(boolean hideOffer) {
        this.hideOffer = hideOffer;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    @Nullable
    @Deprecated
    public Fulfilment getFulfilment() {
        return fulfilment;
    }

    @Deprecated
    public void setFulfilment(Fulfilment fulfilment) {
        this.fulfilment = fulfilment;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }

    public boolean isHideWeight() {
        return hideWeight;
    }

    public void setHideWeight(boolean hideWeight) {
        this.hideWeight = hideWeight;
    }

    public boolean isHideDimensions() {
        return hideDimensions;
    }

    public void setHideDimensions(boolean hideDimensions) {
        this.hideDimensions = hideDimensions;
    }

    public SupplierType getSupplierType() {
        return supplierType;
    }

    public void setSupplierType(SupplierType supplierType) {
        this.supplierType = supplierType;
    }

    public String getWareMd5() {
        return wareMd5;
    }

    public void setWareMd5(String wareMd5) {
        this.wareMd5 = wareMd5;
    }

    public String getFeedGroupIdHash() {
        return feedGroupIdHash;
    }

    public void setFeedGroupIdHash(String feedGroupIdHash) {
        this.feedGroupIdHash = feedGroupIdHash;
    }

    public List<RawParam> getRawParams() {
        return rawParams;
    }

    public void setRawParams(List<RawParam> rawParams) {
        this.rawParams = rawParams;
    }

    public String getShowInfo() {
        return showInfo;
    }

    public void setShowInfo(String showInfo) {
        this.showInfo = showInfo;
    }

    public Boolean getAtSupplierWarehouse() {
        return atSupplierWarehouse;
    }

    public void setAtSupplierWarehouse(Boolean atSupplierWarehouse) {
        this.atSupplierWarehouse = atSupplierWarehouse;
    }

    public long getExternalFeedId() {
        return externalFeedId;
    }

    public void setExternalFeedId(long externalFeedId) {
        this.externalFeedId = externalFeedId;
    }

    public Long getFulfillmentWarehouseId() {
        return fulfillmentWarehouseId;
    }

    public void setFulfillmentWarehouseId(Long fulfillmentWarehouseId) {
        this.fulfillmentWarehouseId = fulfillmentWarehouseId;
    }

    public String getSupplierDescription() {
        return supplierDescription;
    }

    public void setSupplierDescription(String supplierDescription) {
        this.supplierDescription = supplierDescription;
    }

    public List<Region> getManufacturerCountries() {
        return manufacturerCountries;
    }

    public void setManufacturerCountries(List<Region> manufacturerCountries) {
        this.manufacturerCountries = manufacturerCountries;
    }

    public String getSupplierWorkSchedule() {
        return supplierWorkSchedule;
    }

    public void setSupplierWorkSchedule(String supplierWorkSchedule) {
        this.supplierWorkSchedule = supplierWorkSchedule;
    }

    public Boolean isDownloadable() {
        return downloadable;
    }

    public void setDownloadable(Boolean downloadable) {
        this.downloadable = downloadable;
    }

    public PayByYaPlus getPayByYaPlus() {
        return payByYaPlus;
    }

    public void setPayByYaPlus(PayByYaPlus payByYaPlus) {
        this.payByYaPlus = payByYaPlus;
    }

    public static class Prices {

        public BigDecimal value;
        public BigDecimal rawValue;
        public BigDecimal feedPrice;
        public BigDecimal discountOldMin;
        public BigDecimal oldDiscountOldMin;
        public Currency currency;

        public Prices() {
        }

        public Prices(BigDecimal value, BigDecimal rawValue, BigDecimal feedPrice, BigDecimal discountOldMin,
                      Currency currency) {
            this.value = value;
            this.rawValue = rawValue;
            this.feedPrice = feedPrice;
            this.discountOldMin = discountOldMin;
            this.currency = currency;
        }
    }

    @Deprecated
    public static class Fulfilment {

        public Boolean fulfilment;
        public Long supplierId;
        public String sku;
        public String shopSku;
        public Integer warehouseId;

        public Fulfilment() {
        }

        public Fulfilment(Long supplierId, String sku, String shopSku) {
            this(supplierId, sku, shopSku, null, true);
        }

        public Fulfilment(Long supplierId, String sku, String shopSku, Integer warehouseId) {
            this(supplierId, sku, shopSku, warehouseId, true);
        }

        public Fulfilment(Long supplierId, String sku, String shopSku, Integer warehouseId, boolean fulfilment) {
            this.fulfilment = fulfilment;
            this.supplierId = supplierId;
            this.sku = sku;
            this.shopSku = shopSku;
            this.warehouseId = warehouseId;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public String getSku() {
            return sku;
        }

        public String getShopSku() {
            return shopSku;
        }

        public Integer getWarehouseId() {
            return warehouseId;
        }

        public Boolean getFulfilment() {
            return fulfilment;
        }
    }
}
