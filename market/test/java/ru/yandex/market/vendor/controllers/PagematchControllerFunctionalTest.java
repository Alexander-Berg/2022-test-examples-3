package ru.yandex.market.vendor.controllers;

import org.junit.jupiter.api.Test;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

class PagematchControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Test
    void testPostAllProductsEmailSubscriber() {
        String response = FunctionalTestHelper.get(baseUrl + "/pagematch");

        assertThat(
                response,
                containsString("permissionsByVendor_target_uid_\t/permissionsByVendor/<target_uid>\tvendor-partner")
        );
    }

}
