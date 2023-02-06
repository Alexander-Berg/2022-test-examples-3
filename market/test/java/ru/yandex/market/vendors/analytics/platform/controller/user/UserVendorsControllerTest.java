package ru.yandex.market.vendors.analytics.platform.controller.user;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;

import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * Functional tests for {@link UserController#getShops(long)}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "UserVendorsControllerTest.before.csv")
public class UserVendorsControllerTest extends BalanceFunctionalTest {

    @Test
    @DisplayName("Пользователь не может управлять вендорами")
    void userCantManageVendors() {
        mockUserBalanceVendors(1500, Collections.emptySet());
        var response = getManageableVendors(1500);
        var expected = "[]";
        assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь имеет доступ к управлению двумя вендорами")
    void userManageTwoVendors() {
        mockUserBalanceVendors(1501, Set.of(12L));
        var response = getManageableVendors(1501);

        var expected = "[{\"id\": 11, \"name\": \"VND\"}, {\"id\": 12, \"name\": \"Vendor\"}]";
        assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь имеет доступ к упралению отключенным вендором")
    void userManageVendorWithCutoff() {
        mockUserBalanceVendors(1502, Set.of(10L));
        var response = getManageableVendors(1502);
        var expected = "[{\"id\": 10, \"name\": \"\"}]";
        assertEquals(expected, response);
    }

    @Test
    @DisplayName("Не учитываем неоффертного вендора")
    void userManageOnlyNonOfferVendor() {
        mockUserBalanceVendors(1503, Set.of(400L));
        var response = getManageableVendors(1503);
        var expected = "[]";
        assertEquals(expected, response);
    }

    @Test
    @DisplayName("Все офертные вендора пользователя")
    void userOfferVendors() {
        mockUserBalanceVendors(1500, Collections.emptySet());
        var response = getOfferVendors(1500);
        var expected = "[{\"id\": 11, \"name\": \"VND\"}]";
        assertEquals(expected, response);
    }

    @Test
    @DisplayName("У пользователя есть контрактный вендора")
    void userHasContractVendors() {
        var response = hasContractVendors(1503);
        var expected = "true";
        assertEquals(expected, response);
    }

    @Test
    @DisplayName("У пользователя нет контрактного вендора")
    void userHasNoContractVendors() {
        var response = hasContractVendors(1500);
        var expected = "false";
        assertEquals(expected, response);
    }

    private String getManageableVendors(long userId) {
        var shopsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/manageableVendorsInfo")
                .buildAndExpand(userId)
                .toUriString();
        return FunctionalTestHelper.get(shopsUrl).getBody();
    }

    private String getOfferVendors(long userId) {
        var shopsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/offerVendorsInfo")
                .buildAndExpand(userId)
                .toUriString();
        return FunctionalTestHelper.get(shopsUrl).getBody();
    }

    private String hasContractVendors(long userId) {
        var shopsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/hasContractVendors")
                .buildAndExpand(userId)
                .toUriString();
        return FunctionalTestHelper.get(shopsUrl).getBody();
    }
}
