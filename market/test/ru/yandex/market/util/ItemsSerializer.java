package ru.yandex.market.util;

import ru.yandex.market.checkout.common.xml.AbstractXmlSerializer;
import ru.yandex.market.shopadminstub.model.Item;

import java.io.IOException;
import java.util.Collection;

public abstract class ItemsSerializer {
    private ItemsSerializer() {
        throw new UnsupportedOperationException();
    }

    public static void writeItems(AbstractXmlSerializer.PrimitiveXmlWriter writer, Collection<Item> items) throws IOException {
        if (items != null) {
            writer.startNode("items");
            for (Item item : items) {
                writer.startNode("item");
                writer.setAttribute("offer-id", item.getOfferId());
                writer.setAttribute("feed-id", item.getFeedId());
                writer.setAttribute("price", item.getPrice());
                writer.setAttribute("count", item.getCount());
                writer.setAttribute("fulfilment-shop-id", item.getFulfilmentShopId());
                writer.setAttribute("sku", item.getSku());
                writer.setAttribute("shop-sku", item.getShopSku());
                writer.setAttribute("warehouse-id", item.getWarehouseId());
                writer.endNode();
            }
            writer.endNode();
        }
    }
}
