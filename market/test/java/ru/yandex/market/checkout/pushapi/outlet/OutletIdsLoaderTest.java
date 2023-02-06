package ru.yandex.market.checkout.pushapi.outlet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.outlet.MarketOutletId;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletMappingLoadException;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletsMapping;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletId;
import ru.yandex.market.outlet.ShopOutletOuterClass;
import ru.yandex.market.protobuf.tools.NumberConvertionUtils;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class OutletIdsLoaderTest {

    @Test
    void test() throws IOException, OutletMappingLoadException {

        var tmp = Files.createTempFile("outlet-ids", ".test.pb");

        try {

            try (var file = Files.newOutputStream(tmp);
                 var buf = new BufferedOutputStream(file)) {
                for (var outlet : outlets()) {
                    var msg = outlet.toByteArray();
                    buf.write(NumberConvertionUtils.toByteArrayInReversedOrder(msg.length));
                    buf.write(msg);
                }
            }

            var loader = new OutletIdsLoader(tmp.toUri().toURL(), 16);
            OutletsMapping mapping = loader.load();
            Assertions.assertEquals(3, mapping.size());
            Assertions.assertEquals(100L, mapping.getMarketByShop(new ShopOutletId(1, "10")).getId());
            Assertions.assertEquals(200L, mapping.getMarketByShop(new ShopOutletId(2, "10")).getId());
            Assertions.assertEquals(300L, mapping.getMarketByShop(new ShopOutletId(3, "30")).getId());

            Assertions.assertEquals(new ShopOutletId(1, "10"), mapping.getShopByMarket(new MarketOutletId(1, 100)));
            Assertions.assertEquals(new ShopOutletId(2, "10"), mapping.getShopByMarket(new MarketOutletId(2, 200)));
            Assertions.assertEquals(new ShopOutletId(3, "30"), mapping.getShopByMarket(new MarketOutletId(3, 300)));
        } finally {
            Files.deleteIfExists(tmp);
        }

    }

    private List<ShopOutletOuterClass.ShopOutlet> outlets() {
        return List.of(
                outlet(1, "10", 100),
                outlet(2, "10", 200),
                outlet(3, "30", 300)
        );
    }

    private ShopOutletOuterClass.ShopOutlet outlet(long shopId, String shopOutletId, long marketOutletId) {
        return outlet(shopId, shopOutletId, marketOutletId, ShopOutletOuterClass.OutletType.MIXED);
    }

    private ShopOutletOuterClass.ShopOutlet outlet(long shopId,
                                                   String shopOutletId,
                                                   long marketOutletId,
                                                   ShopOutletOuterClass.OutletType type) {
        return ShopOutletOuterClass.ShopOutlet.newBuilder()
                .setOutletType(type)
                .setIds(ShopOutletOuterClass.OutletIds.newBuilder()
                        .setShopId(shopId)
                        .setShopOutletId(shopOutletId)
                        .setMarketOutletId(marketOutletId)
                )
                .build();
    }

}
