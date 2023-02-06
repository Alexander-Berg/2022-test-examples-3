package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.common.report.model.FeedOfferId;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MultiCartOrderItemBuilder extends RandomBuilder<OrderItem> {

    OrderItem item = new OrderItem();

    @Override
    public MultiCartOrderItemBuilder random() {
        item.setFeedOfferId(new FeedOfferId(random.getString(), (long) random.getInt(Integer.MAX_VALUE)));
        BigDecimal price = random.getPrice(100, 100);
        item.setPrice(price);
        item.setBuyerPrice(price);
        item.setCount(random.getInt(10));
        return this;
    }

    public MultiCartOrderItemBuilder withFeedOfferId(String offerId, long feedId) {
        item.setFeedOfferId(new FeedOfferId(offerId, feedId));
        return this;
    }

    public MultiCartOrderItemBuilder withOfferId(String offerId) {
        item.setOfferId(offerId);
        return this;
    }

    public MultiCartOrderItemBuilder withShowInfo(String showInfo) {
        item.setShowInfo(showInfo);
        return this;
    }

    public MultiCartOrderItemBuilder withWareMd5(String offerId) {
        item.setWareMd5(offerId);
        return this;
    }

    public MultiCartOrderItemBuilder withFeedId(long feedId) {
        item.setFeedId(feedId);
        return this;
    }

    public MultiCartOrderItemBuilder withCount(int count) {
        item.setCount(count);
        return this;
    }

    public MultiCartOrderItemBuilder withPromos(Set<ItemPromo> promos) {
        item.setPromos(promos);
        return this;
    }

    @Override
    public OrderItem build() {
        return item;
    }
}
