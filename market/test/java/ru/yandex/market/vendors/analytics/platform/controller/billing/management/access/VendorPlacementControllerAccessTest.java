package ru.yandex.market.vendors.analytics.platform.controller.billing.management.access;

import java.util.List;

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
@DbUnitDataSet(before = "roles.before.csv")
public class VendorPlacementControllerAccessTest extends BalanceFunctionalTest {

    @Test
    @DisplayName("У пользователя нет доступа к дашбордам вендора")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
    void noAccess() {
        mockUserBalanceVendors(10, List.of(10L));
        long uid = 10;
        String response = executeGetCategoriesAccessRequest(1, uid);
        String expected = "{\n"
                + "  \"vendorCategoriesAccess\": \"NO_ACCESS_TO_PARTNER\"\n"
                + "}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("У вендора нет активных категорий")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
    void noActiveCategories() {
        long uid = 11;
        String response = executeGetCategoriesAccessRequest(2, uid);
        String expected = "{\n"
                + "  \"vendorCategoriesAccess\": \"NO_AVAILABLE_CATEGORIES\"\n"
                + "}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("У вендора есть активные категории")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
    void hasActiveCategories() {
        long uid = 12;
        String response = executeGetCategoriesAccessRequest(1, uid);
        String expected = "{\n"
                + "  \"vendorCategoriesAccess\": \"HAS_AVAILABLE_CATEGORIES\"\n"
                + "}";
        assertJsonEquals(expected, response);
    }

    private String executeGetCategoriesAccessRequest(long vendorId, long uid) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "access")
                .queryParam("uid", uid)
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

}
