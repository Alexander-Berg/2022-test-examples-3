package ru.yandex.autotests.market.checkouter.beans.testdata;

import org.apache.commons.lang3.StringUtils;
import ru.yandex.autotests.market.checkouter.beans.BaseBean;
import ru.yandex.autotests.market.checkouter.beans.Changes;
import ru.yandex.autotests.market.checkouter.beans.VatType;
import ru.yandex.autotests.market.checkouter.beans.common.ItemPromo;
import ru.yandex.autotests.market.checkouter.beans.common.Picture;

import java.util.List;

/**
 * Created by belmatter on 10.10.14.
 */
public class TestDataItem extends BaseBean {

    private Long id;

    private Long feedId;

    private String offerId;

    private String wareMd5;

    private String feedCategoryId;

    private Long categoryId;

    private Long modelId;

    private String offerName;

    private String description;

    private String pictureUrl;

    private Double price;

    private List<Picture> pictures;

    private Double buyerPrice;

    private Double buyerPriceBeforeDiscount;

    private Double buyerDiscount;

    private String showInfo;

    private Integer count;

    private Double fee;

    private Double feeSum;

    private Boolean delivery;

    private List<Changes> changes;

    private String showUid;

    private String realShowUid;

    private VatType vat;

    private String balanceOrderId;

    private Long supplierId;

    private String sku;

    private String shopSku;

    private List<ItemPromo> promos;

    private Integer cargoType;

    private String itemDescriptionEnglish;
    private Integer warehouseId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFeedId() {
        return feedId;
    }

    public void setFeedId(Long feedId) {
        this.feedId = feedId;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getWareMd5() {
        return wareMd5;
    }

    public void setWareMd5(String wareMd5) {
        this.wareMd5 = wareMd5;
    }

    public String getFeedCategoryId() {
        return feedCategoryId;
    }

    public void setFeedCategoryId(String feedCategoryId) {
        this.feedCategoryId = feedCategoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    public Double getBuyerPrice() {
        return buyerPrice;
    }

    public void setBuyerPrice(Double buyerPrice) {
        this.buyerPrice = buyerPrice;
    }

    public String getShowInfo() {
        return showInfo;
    }

    public void setShowInfo(String showInfo) {
        this.showInfo = showInfo;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public void setFee(String fee) {
        this.fee = StringUtils.isEmpty(fee) ? null : Double.valueOf(fee);
    }

    public Double getFeeSum() {
        return feeSum;
    }

    public void setFeeSum(Double feeSum) {
        this.feeSum = feeSum;
    }

    public Boolean getDelivery() {
        return delivery;
    }

    public void setDelivery(Boolean delivery) {
        this.delivery = delivery;
    }

    public List<Changes> getChanges() {
        return changes;
    }

    public void setChanges(List<Changes> changes) {
        this.changes = changes;
    }

    public String getShowUid() {
        return showUid;
    }

    public void setShowUid(String showUid) {
        if (!StringUtils.isEmpty(showUid)) {
            this.showUid = showUid;
        }
    }

    public String getRealShowUid() {
        return realShowUid;
    }

    public void setRealShowUid(String realShowUid) {
        this.realShowUid = realShowUid;
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

    public List<ItemPromo> getPromos() {
        return promos;
    }

    public void setPromos(List<ItemPromo> promos) {
        this.promos = promos;
    }

    public Double getBuyerPriceBeforeDiscount() {
        return buyerPriceBeforeDiscount;
    }

    public void setBuyerPriceBeforeDiscount(Double buyerPriceBeforeDiscount) {
        this.buyerPriceBeforeDiscount = buyerPriceBeforeDiscount;
    }

    public Double getBuyerDiscount() {
        return buyerDiscount;
    }

    public void setBuyerDiscount(Double buyerDiscount) {
        this.buyerDiscount = buyerDiscount;
    }

    public TestDataItem() {
    }

    public TestDataItem(Long id, Long feedId, String offerId, String wareMd5, String feedCategoryId, Long categoryId,
                        Long modelId, String offerName, String description, String pictureUrl, Double price,
                        Double buyerPriceBeforeDiscount, Double buyerDiscount,
                        List<Picture> pictures, Double buyerPrice, String showInfo, Integer count, Double fee,
                        Double feeSum, Boolean delivery, List<Changes> changes, String showUid, String realShowUid,
                        VatType vat, Long supplierId, String sku, String shopSku, List<ItemPromo> promos) {
        this.id = id;
        this.feedId = feedId;
        this.offerId = offerId;
        this.wareMd5 = wareMd5;
        this.feedCategoryId = feedCategoryId;
        this.categoryId = categoryId;
        this.modelId = modelId;
        this.offerName = offerName;
        this.description = description;
        this.pictureUrl = pictureUrl;
        this.price = price;
        this.pictures = pictures;
        this.buyerPrice = buyerPrice;
        this.showInfo = showInfo;
        this.count = count;
        this.fee = fee;
        this.feeSum = feeSum;
        this.delivery = delivery;
        this.changes = changes;
        this.showUid = showUid;
        this.realShowUid = realShowUid;
        this.vat = vat;
        this.supplierId = supplierId;
        this.sku = sku;
        this.shopSku = shopSku;
        this.promos = promos;
        this.buyerDiscount = buyerDiscount;
        this.buyerPriceBeforeDiscount = buyerPriceBeforeDiscount;
    }

    public TestDataItemBuilder but() {
        return new TestDataItemBuilder().copy(this);
    }

    public static TestDataItemBuilder builder() {
        return new TestDataItemBuilder();
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getShopSku() {
        return shopSku;
    }

    public void setShopSku(String shopSku) {
        this.shopSku = shopSku;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getCargoType() {
        return cargoType;
    }

    public void setCargoType(Integer cargoType) {
        this.cargoType = cargoType;
    }

    public String getItemDescriptionEnglish() {
        return itemDescriptionEnglish;
    }

    public void setItemDescriptionEnglish(String itemDescriptionEnglish) {
        this.itemDescriptionEnglish = itemDescriptionEnglish;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    //<editor-fold desc="Builder">
    public static class TestDataItemBuilder implements Cloneable {

        private TestDataItemBuilder self;

        private Long id;

        private Long feedId;

        private String offerId;

        private String wareMd5;

        private String feedCategoryId;

        private Long categoryId;

        private Long valueModelId;

        private String offerName;

        private String description;

        private String pictureUrl;

        private Double price;

        private List<Picture> pictures;

        private Double buyerPrice;

        private String showInfo;

        private Integer count;

        private Double fee;

        private Double feeSum;

        private Boolean delivery;

        private List<Changes> changes;

        private String showUid;

        private String realShowUid;

        private VatType vat;

        private Long supplierId;

        private String sku;

        private String shopSku;

        private List<ItemPromo> promos;

        private Double buyerPriceBeforeDiscount;

        private Double buyerDiscount;

        private TestDataItemBuilder() {
            self = this;
        }

        public TestDataItemBuilder withId(Long value) {
            this.id = value;
            return self;
        }

        public TestDataItemBuilder withFeedId(Long value) {
            this.feedId = value;
            return self;
        }

        public TestDataItemBuilder withOfferId(String value) {
            this.offerId = value;
            return self;
        }

        public TestDataItemBuilder withWareMd5(String value) {
            this.wareMd5 = value;
            return self;
        }

        public TestDataItemBuilder withFeedCategoryId(String value) {
            this.feedCategoryId = value;
            return self;
        }

        public TestDataItemBuilder withCategoryId(Long value) {
            this.categoryId = value;
            return self;
        }

        public TestDataItemBuilder withModelId(Long value) {
            this.valueModelId = value;
            return self;
        }

        public TestDataItemBuilder withOfferName(String value) {
            this.offerName = value;
            return self;
        }

        public TestDataItemBuilder withDescription(String value) {
            this.description = value;
            return self;
        }

        public TestDataItemBuilder withPictureUrl(String value) {
            this.pictureUrl = value;
            return self;
        }

        public TestDataItemBuilder withPrice(Double value) {
            this.price = value;
            return self;
        }

        public TestDataItemBuilder withPictures(List<Picture> value) {
            this.pictures = value;
            return self;
        }

        public TestDataItemBuilder withBuyerPrice(Double value) {
            this.buyerPrice = value;
            return self;
        }

        public TestDataItemBuilder withShowInfo(String value) {
            this.showInfo = value;
            return self;
        }

        public TestDataItemBuilder withCount(Integer value) {
            this.count = value;
            return self;
        }

        public TestDataItemBuilder withFee(Double value) {
            this.fee = value;
            return self;
        }

        public TestDataItemBuilder withFeeSum(Double value) {
            this.feeSum = value;
            return self;
        }

        public TestDataItemBuilder withDelivery(Boolean value) {
            this.delivery = value;
            return self;
        }

        public TestDataItemBuilder withChanges(List<Changes> value) {
            this.changes = value;
            return self;
        }

        public TestDataItemBuilder withShowUid(String value) {
            this.showUid = value;
            return self;
        }

        public TestDataItemBuilder withRealShowUid(String value) {
            this.realShowUid = value;
            return self;
        }

        public TestDataItemBuilder withVat(VatType value) {
            this.vat = value;
            return self;
        }

        public TestDataItemBuilder withSku(String value) {
            this.sku = value;
            return self;
        }

        public TestDataItemBuilder withShopSku(String value) {
            this.shopSku = value;
            return self;
        }

        public TestDataItemBuilder withSupplierId(Long value) {
            this.supplierId = value;
            return self;
        }

        public TestDataItemBuilder withPromos(List<ItemPromo> promos) {
            this.promos = promos;
            return this;
        }

        public TestDataItemBuilder withBuyerDiscount(Double buyerDiscount) {
            this.buyerDiscount = buyerDiscount;
            return this;
        }

        public TestDataItemBuilder withBuyerPriceBeforeDiscount(Double buyerPriceBeforeDiscount) {
            this.buyerPriceBeforeDiscount = buyerPriceBeforeDiscount;
            return this;
        }

        @Override
        public Object clone() {
            try {
                TestDataItemBuilder result = (TestDataItemBuilder) super.clone();
                result.self = result;
                return result;
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e.getMessage());
            }
        }

        public TestDataItemBuilder but() {
            return (TestDataItemBuilder) clone();
        }

        public TestDataItemBuilder copy(TestDataItem testDataItem) {
            withId(testDataItem.getId());
            withFeedId(testDataItem.getFeedId());
            withOfferId(testDataItem.getOfferId());
            withWareMd5(testDataItem.getWareMd5());
            withFeedCategoryId(testDataItem.getFeedCategoryId());
            withCategoryId(testDataItem.getCategoryId());
            withModelId(testDataItem.getModelId());
            withOfferName(testDataItem.getOfferName());
            withDescription(testDataItem.getDescription());
            withPictureUrl(testDataItem.getPictureUrl());
            withPrice(testDataItem.getPrice());
            withPictures(testDataItem.getPictures());
            withBuyerPrice(testDataItem.getBuyerPrice());
            withShowInfo(testDataItem.getShowInfo());
            withCount(testDataItem.getCount());
            withFee(testDataItem.getFee());
            withFeeSum(testDataItem.getFeeSum());
            withDelivery(testDataItem.getDelivery());
            withChanges(testDataItem.getChanges());
            withShowUid(testDataItem.getShowUid());
            withRealShowUid(testDataItem.getRealShowUid());
            withVat(testDataItem.getVat());
            withShopSku(testDataItem.getShopSku());
            withSku(testDataItem.getSku());
            withSupplierId(testDataItem.getSupplierId());
            withPromos(testDataItem.getPromos());
            return self;
        }

        public TestDataItem build() {
            return new TestDataItem(
                    id,
                    feedId,
                    offerId,
                    wareMd5,
                    feedCategoryId,
                    categoryId,
                    valueModelId,
                    offerName,
                    description,
                    pictureUrl,
                    price,
                    buyerPriceBeforeDiscount,
                    buyerDiscount,
                    pictures,
                    buyerPrice,
                    showInfo,
                    count,
                    fee,
                    feeSum,
                    delivery,
                    changes,
                    showUid,
                    realShowUid,
                    vat,
                    supplierId,
                    sku,
                    shopSku,
                    promos);
        }

    }
    //</editor-fold>
}
