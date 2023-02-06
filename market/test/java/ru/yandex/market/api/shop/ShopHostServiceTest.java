package ru.yandex.market.api.shop;

import java.util.Arrays;

import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.shop.ShopHostRepository.ShopHostInfo;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShopHostServiceTest extends UnitTestBase {

    private ShopHostService service;

    @Before
    public void start() {
        ShopHostRepositoryFileSupplier supplierProxy = mock(ShopHostRepositoryFileSupplier.class);
        when(supplierProxy.get()).thenReturn(new ShopHostRepositoryImpl(){{
            add(info("www.site1.ru", 1, 1));
            add(info("site2.ru", 2, 2));
            add(info("ww.site3.ru", 3, 3));
            add(info("UPPERcaseSITE.RU", 4, 4));
        }});

        service = new ShopHostService(supplierProxy);
    }

    @Test
    public void shouldFindHostWithOutWwwWhenHostInDbWithWww() {
        Long id = service.findOnlyOneShop("site1.ru", 0);
        Assert.assertEquals(Long.valueOf(1), id);
    }

    @Test
    public void shouldFindHostWithOutWwwWhenHostInDbWithoutWww() {
        Long id = service.findOnlyOneShop("site2.ru", 0);
        Assert.assertEquals(Long.valueOf(2), id);
    }

    @Test
    public void shouldFindHostWithWwWhenWhenHostInDbWithWw() {
        Long id = service.findOnlyOneShop("ww.site3.ru", 0);
        Assert.assertEquals(Long.valueOf(3), id);
    }

    @Test
    public void shouldFindHostWithWwwWhenHostInDbWithWww() {
        Long id = service.findOnlyOneShop("www.site1.ru", 0);
        Assert.assertEquals(Long.valueOf(1), id);
    }

    @Test
    public void shouldNotFindHostWithoutWwWhenWhenHostInDbWithWw() {
        Long id = service.findOnlyOneShop("site3.ru", 0);
        Assert.assertEquals(Long.valueOf(3L), id);
    }

    @Test
    public void shouldNotFoundHost() {
        Long id = service.findOnlyOneShop("www", 0);
        Assert.assertTrue(ShopUtils.isNotFound(id));
    }

    @Test
    public void shouldFindShopWithUpperCase() {
        Long id = service.findOnlyOneShop("upperCASEsite.ru", 0);
        Assert.assertFalse(ShopUtils.isNotFound(id));
    }

    @Test
    public void shouldFindManyShopsByManyHosts() {
        LongList shopsByHosts = service.findShopsByHosts(Arrays.asList("site1.ru", "site2.ru"), 0);
        Assert.assertThat(shopsByHosts, containsInAnyOrder(1L, 2L));
    }

    private ShopHostInfo info(String host, long shopId, int regionId) {
        return new ShopHostInfo(host, regionId, shopId);
    }
}
