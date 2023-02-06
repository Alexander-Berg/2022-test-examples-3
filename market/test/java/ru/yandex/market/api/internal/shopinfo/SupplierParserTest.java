package ru.yandex.market.api.internal.shopinfo;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.Organization;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.shop.OrganizationType;
import ru.yandex.market.api.util.ResourceHelpers;

public class SupplierParserTest extends UnitTestBase {
    @Test
    public void shopParse() {
        ShopInfoV2 shop = supplierParse("shop.json");

        Assert.assertEquals(774L, shop.getId());
        Assert.assertEquals("my shop 1", shop.getName());
        Assert.assertEquals("Пн-Пт: 10:00-20:00, Сб-Вс: 10:00-20:00", shop.getWorkSchedule());
        Assert.assertThat(shop.getOrganizations(), Matchers.hasSize(1));
        Organization org = shop.getOrganizations().get(0);

        Assert.assertEquals("orgName 1", org.getName());
        Assert.assertEquals("12345", org.getOgrn());
        Assert.assertEquals(OrganizationType.OOO, org.getType());
        Assert.assertEquals("jurAddrr 1", org.getAddress());
        Assert.assertEquals("factAddr 1", org.getPostalAddress());
        Assert.assertEquals("8 800 234-27-12", org.getContactPhone());
    }

    @Test
    public void supplerParse() {
        ShopInfoV2 supplier = supplierParse("supplier.json");

        Assert.assertEquals(774L, supplier.getId());
        Assert.assertEquals("my shop 1", supplier.getName());
        Assert.assertEquals("Пн-Пт: 10:00-20:00, Сб-Вс: 10:00-20:00", supplier.getWorkSchedule());
        Assert.assertThat(supplier.getOrganizations(), Matchers.hasSize(1));
        Organization org = supplier.getOrganizations().get(0);

        Assert.assertEquals("orgName 1", org.getName());
        Assert.assertEquals("12345", org.getOgrn());
        Assert.assertEquals(OrganizationType.OOO, org.getType());
        Assert.assertEquals("jurAddrr 1", org.getAddress());
        Assert.assertEquals("factAddr 1", org.getPostalAddress());
        Assert.assertEquals("8 800 234-27-12", org.getContactPhone());
    }

    @Test
    public void supplerSingleParse() {
        ShopInfoV2 supplier = supplierSingleParse("supplier-single.json");

        Assert.assertEquals(774L, supplier.getId());
    }

    @Test
    public void supplerSingleParseWhenEmpty() {
        ShopInfoV2 supplier = supplierSingleParse("supplier-empty.json");

        Assert.assertThat(supplier, Matchers.nullValue());
    }

    private ShopInfoV2 supplierParse(String filename) {
        return new SupplierParser().parse(ResourceHelpers.getResource(filename));
    }

    private ShopInfoV2 supplierSingleParse(String filename) {
        return new SupplierSingleParser().parse(ResourceHelpers.getResource(filename));
    }
}
