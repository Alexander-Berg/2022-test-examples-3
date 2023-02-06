package ru.yandex.market.vendors.analytics.platform.controller.billing.management.prolong;

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
@DbUnitDataSet(before = "../../Balance.common.before.csv")
@DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
public class VendorPlacementControllerProlongTest extends FunctionalTest {

    @Test
    @DisplayName("Успешная отмена отключения категории")
    @DbUnitDataSet(after = "VendorPlacementControllerProlongTest.successfullyProlongCategory.after.csv")
    void successfullyProlongCategory() {
        executeVendorProlongCategoryRequest(1, 14334315, 1001);
    }

    @Test
    @DisplayName("Нельзя отменить отключение категории, для которой не задана конечная дата")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void categoryHasNoTillDate() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorProlongCategoryRequest(1, 91122, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"CAN_NOT_PROLONG_TARIFF_WITH_NO_TILL_DATE\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Нельзя отменить отключение категории, для которой задан следующий тариф")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void categoryHasNextTariff() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorProlongCategoryRequest(1, 6290268, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"CAN_NOT_PROLONG_TARIFF_NEXT_EXISTS\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Нельзя отменить отключение неактивной категории")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void categoryIsInactive() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorProlongCategoryRequest(1, 7286125, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"HAS_NO_ACTIVE_TARIFF\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Последний месяц ещё не был обиллен, отмена отключения категории недоступна")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void lastMonthNotBilled() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorProlongCategoryRequest(2, 7286125, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"BILLING_HAS_NOT_BEEN_PROCESSED_YET\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    private void executeVendorProlongCategoryRequest(long vendorId, long categoryId, long uid) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "{categoryId}", "prolong")
                .queryParam("uid", uid)
                .buildAndExpand(vendorId, categoryId)
                .toUriString();
        FunctionalTestHelper.putForJson(url, null);
    }

}
