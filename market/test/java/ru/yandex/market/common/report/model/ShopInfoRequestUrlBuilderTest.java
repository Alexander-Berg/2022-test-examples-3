package ru.yandex.market.common.report.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ShopInfoRequestUrlBuilderTest {

    @Test
    public void shouldProvideClientAndCoFromIfSpecified() {
        ShopInfoRequestUrlBuilder asdsd = new ShopInfoRequestUrlBuilder("asdsd");
        asdsd.setClient("checkout");
        asdsd.setCoFrom("checkouter");
        String build = asdsd.build(new ShopInfoRequest(Collections.singletonList(774L)));

        Assert.assertTrue(build.contains("client=checkout"));
        Assert.assertTrue(build.contains("co-from=checkouter"));
    }

    @Test
    public void shouldNotProvideClientAndCoFromIfNotSpecified() {
        ShopInfoRequestUrlBuilder asdsd = new ShopInfoRequestUrlBuilder("asdsd");
        String build = asdsd.build(new ShopInfoRequest(Collections.singletonList(774L)));

        Assert.assertFalse(build.contains("client="));
        Assert.assertFalse(build.contains("co-from="));
    }

}
