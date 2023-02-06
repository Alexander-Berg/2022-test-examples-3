package ru.yandex.market.vendors.analytics.platform.controller.billing.management.frozen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.billing.management.VendorPlacementController;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

/**
 * Functional tests for {@link VendorPlacementController#nextMonthFrozenCategories(long, LanguageDTO)}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "../../Balance.common.before.csv")
@DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
public class NextMonthFrozenCategoriesTest extends FunctionalTest {

    @Test
    @DisplayName("Список замороженных категорий офертного вендора")
    void frozenCategories() {
        String response = getVendorFrozenCategories(1);
        String expected = loadFromFile("NextMonthFrozenCategoriesTest.frozenCategories.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Пустой список замороженных категорий офертного вендора")
    void noFrozenCategories() {
        String response = getVendorFrozenCategories(2);
        String expected = loadFromFile("NextMonthFrozenCategoriesTest.noFrozenCategories.response.json");
        assertJsonEquals(expected, response);
    }

    private String getVendorFrozenCategories(long vendorId) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "frozen")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

}
