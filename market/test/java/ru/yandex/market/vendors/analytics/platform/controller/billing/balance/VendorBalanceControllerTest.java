package ru.yandex.market.vendors.analytics.platform.controller.billing.balance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "../OfferClock.2020-04-16.before.csv")
@DbUnitDataSet(before = "../Balance.common.before.csv")
class VendorBalanceControllerTest extends BalanceFunctionalTest {

    @Test
    @DisplayName("Получение баланса для офертного вендора")
    void balanceForOfferVendor() {
        mockBalance(1001, 10_000_000);
        var response = balanceRequest(1);
        var expected = loadFromFile("VendorBalanceControllerTest.balanceForOfferVendor.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение баланса для офертного вендора с недостатком средст на следующий месяц")
    void balanceForOfferVendorNextMonthLack() {
        mockBalance(1001, 5_000_000);
        var response = balanceRequest(1);
        var expected = loadFromFile("VendorBalanceControllerTest.balanceForOfferVendorNextMonthLack.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение баланса для офертного вендора без подключенных категорий")
    void balanceForOfferVendorWithoutActiveCategories() {
        mockBalance(1002, 100_000);
        var response = balanceRequest(2);
        var expected = loadFromFile("VendorBalanceControllerTest.balanceForOfferVendorWithoutActiveCategories.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение баланса для неофертного вендора")
    void balanceForNonOfferVendor() {
        var exception = assertThrows(HttpClientErrorException.class, () -> balanceRequest(3));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        var expected = "" +
                "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"${json-unit.ignore}\",\n" +
                "   \"entityId\": 3,\n" +
                "   \"entityType\":\"OFFER_ACCEPT\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Офертный вендор может потратить деньги")
    void vendorAllowedToSpendMoney() {
        mockBalance(1001, 10_000_000);
        var response = allowSpendRequest(1, 5_800_000, 68_000);
        var expected = allowSpendResponse(true);
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Офертный вендор не может потратить деньги")
    void vendorCantSpendMoney() {
        mockBalance(1002, 100_000);
        var response = allowSpendRequest(2, 60_000, 40_001);
        var expected = allowSpendResponse(false);
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Попытка проверки возможности траты денег неофертным вендором")
    void allowSpendForNonOfferVendor() {
        var exception = assertThrows(HttpClientErrorException.class, () -> allowSpendRequest(3, 0, 0));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        var expected = "" +
                "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"${json-unit.ignore}\",\n" +
                "   \"entityId\": 3,\n" +
                "   \"entityType\":\"OFFER_ACCEPT\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Недостаток денег на начало текущего месяца")
    void currentMonthLackSum() {
        var response = currentMonthLackSum(5);
        var expected = "{\"currentMonthLackSum\": 10}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Не обилен текущий месяц => ещё нет недостатка средств")
    void currentMonthLackSumNotBilled() {
        var response = currentMonthLackSum(6);
        var expected = "{\"currentMonthLackSum\": 0}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Нет недостатка денег на начало текущего месяца")
    void currentMonthLackSumZero() {
        var response = currentMonthLackSum(7);
        var expected = "{\"currentMonthLackSum\": 0}";
        assertJsonEquals(expected, response);
    }

    private String balanceRequest(long vendorId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("vendors", "{vendorId}", "balance")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String allowSpendRequest(long vendorId, long currentMonthSum, long nextMonthFreezeSum) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("vendors", "{vendorId}", "balance", "canSpend")
                .queryParam("currentMonthSum", currentMonthSum)
                .queryParam("freezeNextMonthSum", nextMonthFreezeSum)
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String currentMonthLackSum(long vendorId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("vendors", "{vendorId}", "balance", "currentMonthLackSum")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private static String allowSpendResponse(boolean allow) {
        return "{\"allow\":" + allow + "}";
    }

}