package ru.yandex.market.vendors.analytics.platform.controller.billing.management.tips;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

/**
 * @author ogonek.
 */
@DbUnitDataSet(before = "../../Balance.common.before.csv")
public class VendorPlacementControllerTipsTest extends BalanceFunctionalTest {

    @Test
    @DisplayName("У вендора неткатегории, но есть деньги")
    void addCategory() {
        mockBalance(1002, 10_000_000);
        String response = executeGetCategoriesAccessRequest(2);
        String expected = "{\"vendorManagementStatus\":\"ADD_CATEGORY\"}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("У вендора нет денег для покупки категории")
    void needRefill() {
        mockBalance(1002, 1_000);
        String response = executeGetCategoriesAccessRequest(2);
        String expected = "{\"vendorManagementStatus\":\"NEED_REFILL\"}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(before = "CustomDate.before.csv")
    @DisplayName("У вендора нет денег для покупки категории в следующем месяце")
    void needRefillNextMonth() {
        mockBalance(1001, 3_788_000);
        String response = executeGetCategoriesAccessRequest(1);
        String expected = "{\"vendorManagementStatus\":\"NEED_REFILL\"}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("У вендора есть категория")
    void allSet() {
        mockBalance(1001, 10_000_000);
        String response = executeGetCategoriesAccessRequest(1);
        String expected = "{\"vendorManagementStatus\":\"ALL_SET\"}";
        assertJsonEquals(expected, response);
    }


    private String executeGetCategoriesAccessRequest(long vendorId) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "tips")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

}
