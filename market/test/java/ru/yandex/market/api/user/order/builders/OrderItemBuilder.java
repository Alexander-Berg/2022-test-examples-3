package ru.yandex.market.api.user.order.builders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.common.report.model.json.common.Region;
import ru.yandex.market.common.report.model.specs.Specs;

public class OrderItemBuilder extends RandomBuilder<OrderItem> {
    private OrderItem item = new OrderItem();

    @Override
    public OrderItemBuilder random() {
        item.setId(random.getLong());
        item.setOfferId(random.getString());
        item.setFeedId(random.getLong());
        item.setWareMd5(random.getString());
        item.setCount(random.getInt());
        return this;
    }

    public OrderItemBuilder withFeedId(long feedId) {
        item.setFeedId(feedId);
        return this;
    }

    public OrderItemBuilder withOfferId(String offerId) {
        item.setOfferId(offerId);
        return this;
    }

    public OrderItemBuilder withPicture(String url) {
        item.setPictureURL(url);
        return this;
    }

    public OrderItemBuilder withOfferPictures(Collection<OfferPicture> pictures) {
        item.setPictures(pictures);
        return this;
    }

    public OrderItemBuilder withSupplierId(long supplierId) {
        item.setSupplierId(supplierId);
        return this;
    }

    public OrderItemBuilder withBuyerPriceNominal(BigDecimal price) {
        item.getPrices().setBuyerPriceNominal(price);
        return this;
    }

    public OrderItemBuilder withSupplierDescription(String description) {
        item.setSupplierDescription(description);
        return this;
    }

    public OrderItemBuilder withManufactCountries(Region ... regions) {
        item.setManufacturerCountries(Arrays.asList(regions));
        return this;
    }

    public OrderItemBuilder withMsku(Long msku) {
        item.setMsku(msku);
        return this;
    }

    public OrderItemBuilder withSku(String sku) {
        item.setSku(sku);
        return this;
    }

    public OrderItemBuilder withBundleId(String bundleId) {
        item.setBundleId(bundleId);
        return this;
    }

    public OrderItemBuilder withLabel(String label) {
        item.setLabel(label);
        return this;
    }

    public OrderItemBuilder withWarehouseId(Integer warehouseId) {
        item.setWarehouseId(warehouseId);
        return this;
    }

    public OrderItemBuilder withFulfilmentWarehouseId(Long fulfilmentWarehouseId) {
        item.setFulfilmentWarehouseId(fulfilmentWarehouseId);
        return this;
    }

    public OrderItemBuilder withModelId(Long modelId) {
        item.setModelId(modelId);
        return this;
    }

    public OrderItemBuilder withBnpl(boolean bnpl) {
        item.setBnpl(bnpl);
        return this;
    }

    public OrderItemBuilder withMedicalSpecsInternal(Specs medicalSpecs) {
        item.setMedicalSpecsInternal(medicalSpecs);
        return this;
    }

    @Override
    public OrderItem build() {
        return item;
    }
}
