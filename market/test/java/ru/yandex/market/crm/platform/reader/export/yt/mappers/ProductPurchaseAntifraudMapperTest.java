package ru.yandex.market.crm.platform.reader.export.yt.mappers;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.ProductPurchaseAntifraud;

import static org.junit.Assert.assertEquals;

public class ProductPurchaseAntifraudMapperTest {

    private ProductPurchaseAntifraudMapper mapper = new ProductPurchaseAntifraudMapper();

    @Test
    public void parseTest() {
        YTreeMapNode row = YTree.mapBuilder()
                .key("passportuid").value("74867100")
                .key("yandexuid").value("1582362261531134718")
                .key("model_id").value(201589027)
                .key("timestamp").value(1563062400000L)
                .key("delivery_days").value(3)
                .key("shop_name").value("OZON.ru")
                .key("shop_domain").value("ozon.ru")
                .buildMap();

        ProductPurchaseAntifraud parsed = mapper.apply(row);
        ProductPurchaseAntifraud expected = ProductPurchaseAntifraud.newBuilder()
                .setKeyUid(Uids.create(UidType.PUID, 74867100))
                .setRgb(RGBType.GREEN)
                .addUid(Uids.create(UidType.YANDEXUID, "1582362261531134718"))
                .setProductId("201589027")
                .setTimestamp(1563062400000L)
                .setDeliveryDays(3)
                .setShopName("OZON.ru")
                .setShopDomain("ozon.ru")
                .build();

        assertEquals(expected, parsed);
    }
}