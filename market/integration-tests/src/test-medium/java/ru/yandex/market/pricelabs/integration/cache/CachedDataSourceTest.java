package ru.yandex.market.pricelabs.integration.cache;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.cache.CachedDataSource;
import ru.yandex.market.pricelabs.cache.CachedShopsLoader;
import ru.yandex.market.pricelabs.integration.AbstractIntegrationSpringConfiguration;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.processing.ShopFeedsArg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

class CachedDataSourceTest extends AbstractIntegrationSpringConfiguration {

    @Autowired
    private CachedDataSource dataSource;

    @Autowired
    private CachedShopsLoader cachedShopsLoader;

    private Shop shop;

    @BeforeEach
    void init() {
        shop = shop(1);
        testControls.initOnce(this.getClass(), () -> {
            testControls.saveShop(shop);
            executors.categories().clearTargetTable();
        });
    }

    @Test
    void getShopRepeat() {
        var shop1 = dataSource.loadShop(1).orElseThrow();
        var shop2 = dataSource.loadShop(1).orElseThrow();

        assertEquals(shop.getShop_id(), shop1.getShop_id());
        assertNotSame(shop, shop1);
        assertSame(shop1, shop2);
    }

    @Test
    void getShopRepeatThroughLoader() {
        var shop1 = cachedShopsLoader.getLoader().getShop(1);
        var shop2 = cachedShopsLoader.getLoader().getShop(1);

        assertEquals(shop.getShop_id(), shop1.getShop_id());
        assertNotSame(shop, shop1);
        assertSame(shop1, shop2);
    }


    @Test
    void testShopNotFound() {
        assertTrue(dataSource.loadShop(999).isEmpty());
    }

    @Test
    void testIsExists() {
        boolean exists = dataSource.loadShop(1).isPresent();
        assertTrue(exists);

        boolean notExists = dataSource.loadShop(999).isPresent();
        assertFalse(notExists);

    }

    @Test
    void getCategoriesRepeat() {
        var cat1 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1L, 1L), false);
        var cat2 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1L, 2L), false);
        var cat11 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1L, 1L), false);
        var cat11W = dataSource.loadCategoriesWhite(new ShopFeedsArg(1L, 1L), true);

        assertTrue(cat1.getCategoriesPerFeed().isEmpty());
        assertTrue(cat2.getCategoriesPerFeed().isEmpty());
        assertTrue(cat11.getCategoriesPerFeed().isEmpty());
        assertTrue(cat11W.getCategoriesPerFeed().isEmpty());

        assertNotSame(cat1, cat2);
        assertSame(cat1, cat11);
        assertNotSame(cat11, cat11W);
    }

    @Test
    void getCategoriesForAll() {
        var cat1 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1, List.of()), false);
        var cat11 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1), false);
        var cat11W = dataSource.loadCategoriesWhite(new ShopFeedsArg(1), true);

        assertTrue(cat1.getCategoriesPerFeed().isEmpty());
        assertTrue(cat11.getCategoriesPerFeed().isEmpty());
        assertTrue(cat11W.getCategoriesPerFeed().isEmpty());

        assertSame(cat1, cat11);
        assertNotSame(cat11, cat11W);
    }

    @Test
    void loadCategoriesLookup() {
        var lookup1 = dataSource.loadCategoriesLookupWhite(1, false).orElseThrow();
        var lookup2 = dataSource.loadCategoriesLookupWhite(1, false).orElseThrow();
        var lookup2W = dataSource.loadCategoriesLookupWhite(1, true).orElseThrow();
        assertSame(lookup1, lookup2);
        assertNotSame(lookup2, lookup2W);
    }

    @Test
    void getCategoriesBlue() {
        var cat1 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1, List.of()), false);
        var cat11 = dataSource.loadCategoriesWhite(new ShopFeedsArg(1), false);
        assertSame(cat1, cat11);

        var catb1 = dataSource.loadCategoriesBlue(new ShopFeedsArg(1, List.of()));
        var catb11 = dataSource.loadCategoriesBlue(new ShopFeedsArg(1));

        assertTrue(catb1.getCategoriesPerFeed().isEmpty());
        assertTrue(catb11.getCategoriesPerFeed().isEmpty());

        assertSame(catb1, catb11);
        assertNotSame(cat1, catb1);
    }

    @Test
    void loadCategoriesLookupBlue() {
        var lookup1 = dataSource.loadCategoriesLookupWhite(1, false).orElseThrow();
        var lookup2 = dataSource.loadCategoriesLookupWhite(1, false).orElseThrow();
        assertSame(lookup1, lookup2);

        var lookupb1 = dataSource.loadCategoriesLookupBlue(1).orElseThrow();
        var lookupb2 = dataSource.loadCategoriesLookupBlue(1).orElseThrow();
        assertSame(lookupb1, lookupb2);
        assertNotSame(lookup1, lookupb2);
    }
}
