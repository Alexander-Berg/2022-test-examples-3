package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.offer.Phone;
import ru.yandex.market.api.user.order.Payload;
import ru.yandex.market.api.user.order.ShopOrderItem;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ShopOrderItemBuilder extends RandomBuilder<ShopOrderItem> {

    private static final String NUMBERS = "0123456789";

    ShopOrderItem item = new ShopOrderItem();

    @Override
    public ShopOrderItemBuilder random() {
        item.setPayload(new Payload(
            (long) random.getInt(Integer.MAX_VALUE),
            random.getString(),
            random.getString(),
            random.getString()
        ));
        item.setMarketOfferId(random.getString());
        item.setCount(random.getInt(1, 100));
        item.setPhone(new Phone(
            random.getString(NUMBERS, 10),
            random.getString(NUMBERS, 10),
            random.getString()
        ));
        item.setPrice(random.getPrice(1000, 0));
        item.setTitle(random.getString());
        return this;
    }

    public ShopOrderItemBuilder withMarketOfferId(String marketOfferId) {
        item.setMarketOfferId(marketOfferId);
        return this;
    }

    public ShopOrderItemBuilder withSupplierId(long supplierId) {
        item.setSupplierId(supplierId);
        return this;
    }

    public ShopOrderItemBuilder withPayload(long feedId, String shopOfferId, String marketOfferId, String feeShow) {
        item.setPayload(new Payload(feedId, shopOfferId, marketOfferId, feeShow));
        return this;
    }

    public ShopOrderItemBuilder withSku(String sku) {
        item.setSku(new Sku.SkuId(sku));
        return this;
    }

    @Override
    public ShopOrderItem build() {
        return item;
    }
}
