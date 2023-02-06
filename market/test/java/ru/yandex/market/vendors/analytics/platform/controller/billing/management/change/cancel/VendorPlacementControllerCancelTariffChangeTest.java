package ru.yandex.market.vendors.analytics.platform.controller.billing.management.change.cancel;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "../../../Balance.common.before.csv")
@DbUnitDataSet(before = "../../../OfferClock.2020-04-16.before.csv")
public class VendorPlacementControllerCancelTariffChangeTest extends FunctionalTest {

    @Test
    @DisplayName("Успешная отмена смены тарифа")
    @DbUnitDataSet(after = "VendorPlacementControllerCancelTariffChangeTest.cancelChangeTariffCategory.after.csv")
    void cancelChangeTariffCategory() {
        executeVendorCancelChangeCategoryRequest(1, 6290268, 1001);
    }

    @Test
    @DisplayName("Новый тариф не задан")
    void hasNoNextTariff() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorCancelChangeCategoryRequest(1, 91122, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"HAS_NO_UPCOMING_TARIFF_CHANGE\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Последний месяц ещё не был обиллен, отмена переключения тарифа недоступна")
    void lastMonthNotBilled() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorCancelChangeCategoryRequest(2, 91122, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"BILLING_HAS_NOT_BEEN_PROCESSED_YET\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    private void executeVendorCancelChangeCategoryRequest(long vendorId, long categoryId, long uid) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "{categoryId}", "change")
                .queryParam("uid", uid)
                .buildAndExpand(vendorId, categoryId)
                .toUriString();
        FunctionalTestHelper.delete(url);
    }

}
