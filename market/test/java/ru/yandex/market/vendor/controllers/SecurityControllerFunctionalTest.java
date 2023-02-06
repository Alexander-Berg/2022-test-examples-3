package ru.yandex.market.vendor.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/SecurityControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/SecurityControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class SecurityControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer blackboxMock;

    @Autowired
    public SecurityControllerFunctionalTest(WireMockServer blackboxMock) {
        this.blackboxMock = blackboxMock;
    }

    @Test
    @DisplayName("Получение списка всех возможных пермишнов")
    void testGetPermissions() {
        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissions?uid=100500");
        String expected = getStringResource("/testGetPermissions/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка всех возможных ролей")
    void testGetAuthorities() {
        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/authorities?uid=100500");
        String expected = getStringResource("/testGetAuthorities/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetAuthoritiesByVendor() {
        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 1000L);
        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/authoritiesByVendor/100500?uid=100500");
        String expected = getStringResource("/testGetAuthoritiesByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetAuthoritiesByVendorAndProductKey() {
        initBalanceServiceByClientId(552);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/authoritiesByVendor/100500/analytics?uid=100500");
        String expected = getStringResource("/testGetAuthoritiesByVendorAndProductKey/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetAuthoritiesUidsVendorRole() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetAuthoritiesUidsVendorRole/blackbox_response.json"))));

        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 1000L);
        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        String actual = FunctionalTestHelper.getWithAuth(
                baseUrl + "/authorities/uids/vendor/1000/role/recommended_shops_user?uid=100500");
        String expected = getStringResource("/testGetAuthoritiesUidsVendorRole/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPutAuthoritiesUidsVendorRole() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testPutAuthoritiesUidsVendorRole" +
                        "/blackbox_response.json"))));

        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        String request = getStringResource("/testPutAuthoritiesUidsVendorRole/request.json");
        String actual = FunctionalTestHelper.putWithAuth(
                baseUrl + "/authorities/uids/vendor/1000/role/recommended_shops_user?uid=100500",
                request);
        String expected = getStringResource("/testPutAuthoritiesUidsVendorRole/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    public void testGetAuthoritiesUidVendor() {
        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 1000L);
        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        String actual = FunctionalTestHelper.getWithAuth(
                baseUrl + "/authorities/100500/vendor/1000?uid=100500");
        String expected = getStringResource("/testGetAuthoritiesUidVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    public void testPutAuthoritiesUidVendor() {
        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        String request = getStringResource("/testPutAuthoritiesUidVendor/request.json");
        String actual = FunctionalTestHelper.putWithAuth(
                baseUrl + "/authorities/9000/vendor/1000?uid=100500",
                request);
        String expected = getStringResource("/testPutAuthoritiesUidVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    public void testDeleteAuthoritiesUidVendor() {
        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        String actual = FunctionalTestHelper.deleteWithAuth(
                baseUrl + "/authorities/9000/vendor/1000?uid=100500&roles=recommended_shops_user");
        String expected = getStringResource("/testDeleteAuthoritiesUidVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("У балансового пользователя (без других ролей) не должно быть пермишна vnd:questions:read")
    void testBalanceUserHasNoQuestionsReadPermission() {
        initBalanceServiceByClientId(550);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/100500?uid=100500");
        String expected = getStringResource("/testBalanceUserHasNoQuestionsReadPermission/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("У балансового пользователя (при наличии других ролей бесплатных услуг по вендору) должен быть " +
            "пермишна vnd:questions:read по этому вендору")
    void testBalanceUserHasNoQuestionsReadPermissionIfHasFreeProductsRolesOfSameVendor() {
        initBalanceServiceByClientId(550);
        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 1000L);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/100500?uid=100500");
        String expected = getStringResource("/testBalanceUserHasNoQuestionsReadPermissionIfHasFreeProductsRolesOfSameVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("У балансового пользователя (при наличии других ролей бесплатных услуг только по другому вендору) " +
            "не должно быть пермишна vnd:questions:read по этому вендору")
    void testBalanceUserHasNoQuestionsReadPermissionIfHasFreeProductsRolesOfOtherVendors() {
        initBalanceServiceByClientId(550);
        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 1001L);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/100500?uid=100500");
        String expected = getStringResource("/testBalanceUserHasNoQuestionsReadPermissionIfHasFreeProductsRolesOfOtherVendors/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка пермишнов для пользователя с захардкоженной ролью")
    void testGetPermissionsByVendorForHardcodedRole() {
        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/100501?uid=100501");
        String expected = getStringResource("/testGetPermissionsByVendorForHardcodedRole/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка пермишнов для пользователя с захардкоженной ролью, совпадающей с IDM-ролью")
    void testGetPermissionsByVendorForHardcodedSameAsIdmRole() {
        setVendorUserRoles(singletonList(Role.manager_user), 100501);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/100501?uid=100501");
        String expected = getStringResource("/testGetPermissionsByVendorForHardcodedRole/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/SecurityControllerFunctionalTest/testGetPermissionsByVendorPaidOpinionUser/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/SecurityControllerFunctionalTest/testGetPermissionsByVendorPaidOpinionUser/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Получение списка пермишнов для пользователя с ролью для платных отзывов")
    void testGetPermissionsByVendorPaidOpinionUser() {
        setVendorUserRoles(singletonList(Role.manager_user), 100501);
        initBalanceServiceByClientId(100500);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/1023?uid=100501");
        String expected = getStringResource("/testGetPermissionsByVendorPaidOpinionUser/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/SecurityControllerFunctionalTest/testGetPermissionsByVendorPaidOpinionUserBalanceClient/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/SecurityControllerFunctionalTest/testGetPermissionsByVendorPaidOpinionUserBalanceClient/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Получение списка пермишнов для пользователя с ролью для платных отзывов и балансовым пользователем")
    void testGetPermissionsByVendorPaidOpinionUserBalanceClient() {
        setVendorUserRoles(singletonList(Role.manager_user), 100501);
        initBalanceServiceByClientId(553);

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/permissionsByVendor/1023?uid=100501");
        String expected = getStringResource("/testGetPermissionsByVendorPaidOpinionUserBalanceClient/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка ролей для пользователя с захардкоженной ролью")
    void testGetAuthoritiesByVendorForHardcodedRole() {
        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/authoritiesByVendor/100501?uid=100501");
        String expected = getStringResource("/testGetAuthoritiesByVendorForHardcodedRole/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
