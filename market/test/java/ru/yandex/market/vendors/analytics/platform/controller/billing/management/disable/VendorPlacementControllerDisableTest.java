package ru.yandex.market.vendors.analytics.platform.controller.billing.management.disable;

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
public class VendorPlacementControllerDisableTest extends FunctionalTest {

    @Test
    @DisplayName("Успешное отключение категории вендора")
    @DbUnitDataSet(after = "VendorPlacementControllerDisableTest.successfullyDisableCategory.after.csv")
    void successfullyDisableCategory() {
        executeVendorDisableCategoryRequest(1, 90796, 1001);
    }

    @Test
    @DisplayName("Успешное отключение категории вендора, для которой был задан следующий тариф")
    @DbUnitDataSet(after = "VendorPlacementControllerDisableTest.disableCategoryWithNextMonthTariff.after.csv")
    void disableCategoryWithNextMonthTariff() {
        executeVendorDisableCategoryRequest(1, 6290268, 1001);
    }

    @Test
    @DisplayName("Нельзя отключить не подключенную категорию")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void categoryAlreadyDisabled() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorDisableCategoryRequest(1, 7286125, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"HAS_NO_ACTIVE_TARIFF\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Нельзя отключить категорию, отключаемую в конце месяца")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void categoryWillDisableNextMonth() {
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeVendorDisableCategoryRequest(1, 14334315, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"FORBIDDEN_TO_DISABLE_CATEGORY\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Нельзя отключить категорию, замороженную из-за поднего подключения")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void disableForbiddenCauseLateAdd() {
        var exception = assertThrows(HttpClientErrorException.class, () -> executeVendorDisableCategoryRequest(1, 91013, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"FORBIDDEN_TO_DISABLE_CATEGORY\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }


    @Test
    @DisplayName("Нельзя отключить категорию, замороженную из-за подней смены тарифа")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void disableForbiddenCauseLateTariffChange() {
        var exception = assertThrows(HttpClientErrorException.class, () -> executeVendorDisableCategoryRequest(1, 10972670, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"FORBIDDEN_TO_DISABLE_CATEGORY\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Последний месяц ещё не был обиллен, отключение категории недоступно")
    @DbUnitDataSet(after = "../../Balance.common.before.csv")
    void lastMonthNotBilled() {
        var exception = assertThrows(HttpClientErrorException.class, () -> executeVendorDisableCategoryRequest(2, 10972670, 1001));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"BILLING_HAS_NOT_BEEN_PROCESSED_YET\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    private void executeVendorDisableCategoryRequest(long vendorId, long categoryId, long uid) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "{categoryId}")
                .queryParam("uid", uid)
                .buildAndExpand(vendorId, categoryId)
                .toUriString();
        FunctionalTestHelper.delete(url);
    }

}
