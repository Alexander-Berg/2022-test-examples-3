package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.pushapi.client.entity.BaseBuilder;
import ru.yandex.market.common.report.model.QuantityLimits;

/**
 * @author msavelyev
 */
public class OrderItemBuilder extends BaseBuilder<OrderItem, OrderItemBuilder> {
    public OrderItemBuilder() {
        super(new OrderItem());

        object.setFeedId(1234L);
        object.setOfferId("2345");
//        object.setCategoryId(3456);
        object.setFeedCategoryId("Камеры");
        object.setOfferName("OfferName");
        object.setPrice(new BigDecimal("4567"));
        object.setCount(5);
        object.setDelivery(true);
    }

    public OrderItemBuilder withId(Long id) {
        return withField("id", id);
    }

    public OrderItemBuilder withFeedId(Long feedId) {
        return withField("feedId", feedId);
    }

    public OrderItemBuilder withOfferId(String offerId) {
        return withField("offerId", offerId);
    }

    public OrderItemBuilder withFeedCategoryId(String feedCategoryId) {
        return withField("feedCategoryId", feedCategoryId);
    }

    public OrderItemBuilder withCategoryId(Integer categoryId) {
        return withField("categoryId", categoryId);
    }

    public OrderItemBuilder withOfferName(String offerName) {
        return withField("offerName", offerName);
    }

    public OrderItemBuilder withPrice(BigDecimal Price) {
        return withField("Price", Price);
    }

    public OrderItemBuilder withSupplierCurrency(Currency supplierCurrency) {
        return withField("supplierCurrency", supplierCurrency);
    }

    public OrderItemBuilder withCount(Integer count) {
        return withField("count", count);
    }

    public OrderItemBuilder withDelivery(Boolean delivery) {
        return withField("delivery", delivery);
    }

    public OrderItemBuilder withSubsidy(long subsidy) {
        object.getPrices().setSubsidy(BigDecimal.valueOf(subsidy));
        return this;
    }

    public OrderItemBuilder withKind2Parameters(List<ItemParameter> kind2Parameters) {
        return withField("kind2Parameters", kind2Parameters);
    }

    public OrderItemBuilder withQuantityLimits(int minimum, int step) {
        QuantityLimits value = new QuantityLimits();
        value.setMinimum(minimum);
        value.setStep(step);
        return withField("quantityLimits", value);
    }

    public OrderItemBuilder withPromo(Set<ItemPromo> promos) {
        return withField("promos", promos);
    }

    public OrderItemBuilder withVat(VatType vat) {
        return withField("vat", vat);
    }

    public OrderItemBuilder withSellerInn(String sellerInn) {
        return withField("sellerInn", sellerInn);
    }
}
