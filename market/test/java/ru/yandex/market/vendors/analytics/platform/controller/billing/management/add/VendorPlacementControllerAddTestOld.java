package ru.yandex.market.vendors.analytics.platform.controller.billing.management.add;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.billing.TariffType;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
class VendorPlacementControllerAddTestOld extends AbstractAddTest {

    @Test
    @DisplayName("Удачное подключение категорий в середине месяца")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
    @DbUnitDataSet(before = "VendorPlacementControllerAddTest.successfullyAddCategoriesMedium.before.csv")
    @DbUnitDataSet(after = "VendorPlacementControllerAddTestOld.successfullyAddCategoriesMedium.after.csv")
    void successfullyAddCategoriesMedium() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 4, 16)));

        long uid = 11;
        mockBalance(1001, 10_000_000);
        mockChangeDynamicCost(1001, uid, 3824500);
        var body = addCategoriesRequest(Map.of(7286125L, TariffType.FULL, 90555L, TariffType.BASIC));
        var response = executeAddCategoriesRequest(1, uid, body);
        var expected =
                loadFromFile("VendorPlacementControllerAddTestOld.successfullyAddCategoriesMedium.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @Disabled
    @DisplayName("Не получилось подключить категории из-за нехватки средств")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
    @DbUnitDataSet(after = "VendorPlacementControllerAddTestOld.addCategoriesInsufficientFunds.after.csv")
    void addCategoriesInsufficientFunds() {
        long uid = 11;
        mockBalance(1001, 3777499);
        var body = addCategoriesRequest(Map.of(7286125L, TariffType.FULL, 90555L, TariffType.BASIC));
        var exception = assertThrows(HttpClientErrorException.class, () -> executeAddCategoriesRequest(1, uid, body));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{\n" +
                "   \"code\":\"INSUFFICIENT_FUNDS\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

}