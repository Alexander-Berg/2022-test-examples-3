package ru.yandex.market.pricelabs.tms.processing.imports;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.model.NewShopsDat;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.ShopFeed;
import ru.yandex.market.pricelabs.model.ShopsDat;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;

public class ShopsDatProcessorTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private ShopsDatProcessor processor;

    //CHECKSTYLE:OFF
    public static NewShopsDat newShopsDat(long shopId, long feedId, long businessId, String shopName, long regionId,
                                          boolean isDsbs, boolean isSupplier, boolean isEnabled) {
        //CHECKSTYLE:ON
        var newShopsDat = new NewShopsDat();
        newShopsDat.setShop_id(shopId);
        newShopsDat.setDatafeed_id(feedId);
        newShopsDat.setBusiness_id(businessId);
        newShopsDat.setShopname(shopName);
        newShopsDat.setDomain(shopName);
        newShopsDat.setShop_currency("");
        newShopsDat.setHome_region(225);
        newShopsDat.setPriority_regions(regionId);
        newShopsDat.set_dsbs(isDsbs);
        newShopsDat.set_supplier(isSupplier);
        newShopsDat.set_enabled(isEnabled);
        return newShopsDat;
    }

    public static ShopsDat shopsDat(int shopId, long businessId, String shopName, int regionId, boolean isDsbs,
                                    Instant updatedAt) {
        var shopsDat = new ShopsDat();
        shopsDat.setShop_id(shopId);
        shopsDat.setBusiness_id(businessId);
        shopsDat.setName(shopName);
        shopsDat.setDomain(shopName);
        shopsDat.setCurrency("");
        shopsDat.setRegion_id(regionId);
        shopsDat.set_dsbs(isDsbs);
        shopsDat.setUpdated_at(updatedAt);
        return shopsDat;
    }

    //CHECKSTYLE:OFF
    public static Shop shop(int shopId, ShopType type, String shopName, long businessId, int regionId,
                            ShopStatus status, Set<Long> feeds, Instant createdAt, Instant updatedAt) {
        //CHECKSTYLE:ON
        var shop = new Shop();
        shop.setShop_id(shopId);
        shop.setType(type);
        shop.setName(shopName);
        shop.setDomain(shopName);
        shop.setBusiness_id(businessId);
        shop.setRegion_id(regionId);
        shop.setStatus(status);
        shop.setFeeds(feeds);
        shop.setCreated_at(createdAt);
        shop.setUpdated_at(updatedAt);
        return shop;
    }

    public static ShopFeed feed(int feedId) {
        var feed = new ShopFeed();
        feed.setFeed_id(feedId);
        feed.setUrl("");
        feed.setStatus(Status.ACTIVE);
        feed.setDetails("");
        return feed;
    }

    @BeforeEach
    void init() {
        executors.shopsDat().clearSourceTable();
        executors.shopsDat().clearTargetTable();
        executors.shop().clearTargetTable();
    }

    @Test
    protected void testImport() {
        executors.shopsDat().insertSource(List.of(
                newShopsDat(1, 1, 1, "shop1", 1, false, false, true),
                newShopsDat(2, 1, 1, "shop2", 1, true, false, false),
                newShopsDat(3, 1, 1, "shop3", 111, true, false, true),
                newShopsDat(3, 2, 1, "shop3", 111, true, false, true),
                newShopsDat(4, 2, 1, "shop4", 222, true, false, true),
                newShopsDat(5, 2, 1, "shop5", 1, false, true, true),
                newShopsDat(ApiConst.VIRTUAL_SHOP_BLUE, 1, 1, "virtual", 213, false, false, true)
        ));
        Instant old = Instant.ofEpochMilli(123);
        executors.shop().insert(List.of(
                shop(1, ShopType.DSBS, "shop1", 1, 1, ShopStatus.ACTIVE, Set.of(1L), old, old),
                shop(3, ShopType.DSBS, "shop3", 1, 1, ShopStatus.INACTIVE, Set.of(1L), old, old)
        ));

        processor.sync("zeno", TmsTestUtils.DEFAULT_INDEXER_NAME, TmsTestUtils.DEFAULT_GENERATION);

        var now = getInstant();
        executors.shopsDat().verify(List.of(
                shopsDat(3, 1, "shop3", 111, true, now),
                shopsDat(4, 1, "shop4", 222, true, now),
                shopsDat(5, 1, "shop5", 1, false, now)
        ));
        executors.shop().verify(List.of(
                shop(1, ShopType.DSBS, "shop1", 1, 1, ShopStatus.INACTIVE, Set.of(1L), old, old),
                shop(3, ShopType.DSBS, "shop3", 1, 111, ShopStatus.ACTIVE, Set.of(1L, 2L), old, now),
                shop(4, ShopType.DSBS, "shop4", 1, 222, ShopStatus.ACTIVE, Set.of(2L), now, now),
                shop(5, ShopType.SUPPLIER, "shop5", 1, 1, ShopStatus.ACTIVE, Set.of(2L), now, now)
        ));
    }
}
