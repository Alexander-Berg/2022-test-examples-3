package ru.yandex.market.checkout.carter.utils.builders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import ru.yandex.market.checkout.carter.json.format.ItemField;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.ItemPromo;

public class ItemOfferBuilder {

    protected long id;
    protected long listId;
    protected CartItem.Type objType;
    protected String objId;
    protected String label;
    protected String name;
    protected Integer count = 1;
    protected String bundleId;
    protected String bundlePromoId;
    protected boolean primaryInBundle = false;
    protected Date createTime;
    protected Date updateTime;
    protected String createRequestId;
    protected String kind2Params;
    protected String pictureUrl;
    protected String promoKey;
    protected Set<String> fieldsToChange = Collections.singleton(ItemField.COUNT);
    private String fee;
    private Long modelId;
    private Long hid;
    private Long shopId;
    private String desc;
    private String outletId;
    private boolean isExpired;
    private Long msku;
    private BigDecimal price;
    private transient Integer warehouseId;
    private boolean adult;
    private Set<ItemPromo> promos;
    private String actualizedObjId;
    private BigDecimal actualizedPrice;

    public ItemOfferBuilder() {
    }

    public ItemOfferBuilder withFee(String fee) {
        this.fee = fee;
        return this;
    }

    public ItemOfferBuilder withModelId(Long modelId) {
        this.modelId = modelId;
        return this;
    }

    public ItemOfferBuilder withHid(Long hid) {
        this.hid = hid;
        return this;
    }

    public ItemOfferBuilder withShopId(Long shopId) {
        this.shopId = shopId;
        return this;
    }

    public ItemOfferBuilder withDesc(String desc) {
        this.desc = desc;
        return this;
    }

    public ItemOfferBuilder withOutletId(String outletId) {
        this.outletId = outletId;
        return this;
    }

    public ItemOfferBuilder withIsExpired(boolean isExpired) {
        this.isExpired = isExpired;
        return this;
    }

    public ItemOfferBuilder withMsku(Long msku) {
        this.msku = msku;
        return this;
    }

    public ItemOfferBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ItemOfferBuilder withWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public ItemOfferBuilder withId(long id) {
        this.id = id;
        return this;
    }

    public ItemOfferBuilder withListId(long listId) {
        this.listId = listId;
        return this;
    }

    public ItemOfferBuilder withObjType(CartItem.Type objType) {
        this.objType = objType;
        return this;
    }

    public ItemOfferBuilder withObjId(String objId) {
        this.objId = objId;
        return this;
    }

    public ItemOfferBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public ItemOfferBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ItemOfferBuilder withCount(Integer count) {
        this.count = count;
        return this;
    }

    public ItemOfferBuilder withBundleId(String bundleId) {
        this.bundleId = bundleId;
        return this;
    }

    public ItemOfferBuilder withBundlePromoId(String bundlePromoId) {
        this.bundlePromoId = bundlePromoId;
        return this;
    }

    public ItemOfferBuilder withPrimaryInBundle(boolean primaryInBundle) {
        this.primaryInBundle = primaryInBundle;
        return this;
    }

    public ItemOfferBuilder withCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public ItemOfferBuilder withUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public ItemOfferBuilder withCreateRequestId(String createRequestId) {
        this.createRequestId = createRequestId;
        return this;
    }

    public ItemOfferBuilder withKind2Params(String kind2Params) {
        this.kind2Params = kind2Params;
        return this;
    }

    public ItemOfferBuilder withPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
        return this;
    }

    public ItemOfferBuilder withPromoKey(String promoKey) {
        this.promoKey = promoKey;
        return this;
    }

    public ItemOfferBuilder withFieldsToChange(Set<String> fieldsToChange) {
        this.fieldsToChange = fieldsToChange;
        return this;
    }

    public ItemOfferBuilder withAdult(boolean adult) {
        this.adult = adult;
        return this;
    }

    public ItemOfferBuilder withPromos(Set<ItemPromo> promos) {
        this.promos = promos;
        return this;
    }

    public ItemOfferBuilder withActualizedPrice(BigDecimal actualizedPrice) {
        this.actualizedPrice = actualizedPrice;
        return this;
    }

    public ItemOfferBuilder withActualizedObjId(String actualizedObjId) {
        this.actualizedObjId = actualizedObjId;
        return this;
    }

    public ItemOffer build() {
        ItemOffer itemOffer = new ItemOffer(objId, name);
        itemOffer.setFee(fee);
        itemOffer.setModelId(modelId);
        itemOffer.setHid(hid);
        itemOffer.setShopId(shopId);
        itemOffer.setDesc(desc);
        itemOffer.setOutletId(outletId);
        itemOffer.setMsku(msku);
        itemOffer.setPrice(price);
        itemOffer.setWarehouseId(warehouseId);
        itemOffer.setId(id);
        itemOffer.setListId(listId);
        itemOffer.setObjType(objType);
        itemOffer.setObjId(objId);
        itemOffer.setLabel(label);
        itemOffer.setName(name);
        itemOffer.setCount(count);
        itemOffer.setBundleId(bundleId);
        itemOffer.setBundlePromoId(bundlePromoId);
        itemOffer.setPrimaryInBundle(primaryInBundle);
        itemOffer.setCreateTime(createTime);
        itemOffer.setUpdateTime(updateTime);
        itemOffer.setCreateRequestId(createRequestId);
        itemOffer.setKind2Params(kind2Params);
        itemOffer.setPictureUrl(pictureUrl);
        itemOffer.setPromoKey(promoKey);
        itemOffer.setFieldsToChange(fieldsToChange);
        itemOffer.setExpired(this.isExpired);
        itemOffer.setAdult(adult);
        itemOffer.setPromos(promos);
        itemOffer.setActualizedObjId(actualizedObjId);
        itemOffer.setActualizedPrice(actualizedPrice);

        return itemOffer;
    }
}
