package ru.yandex.market.vendors.analytics.platform.controller.billing.management.list;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "../../Balance.common.before.csv")
@DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
public class VendorPlacementControllerListTest extends FunctionalTest {

    @Test
    @DisplayName("Список категорий офертного вендора")
    void vendorCategories() {
        String response = getVendorCategories(1);
        String expected = loadFromFile("VendorPlacementControllerListTest.vendorCategories.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Пустой список категорий офертного вендора")
    void vendorWithoutCategories() {
        String response = getVendorCategories(2);
        String expected = loadFromFile("VendorPlacementControllerListTest.vendorWithoutCategories.response.json");
        assertJsonEquals(expected, response);
    }

    private String getVendorCategories(long vendorId) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

}
