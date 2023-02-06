package ru.yandex.market.vendors.analytics.platform.controller.role;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.contact.Role;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "UserPartnerRoleControllerTest.before.csv")
public class UserPartnerRoleControllerTest extends FunctionalTest {

    private static final String ROLES_URL = "/roles";

    @Test
    @DisplayName("Успешная выдача роли")
    @DbUnitDataSet(after = "UserPartnerRoleControllerTest.grantRole.after.csv")
    void grantRole() {
        var expected = ""
                + "{\n"
                + "  \"id\": \"${json-unit.ignore}\",\n"
                + "  \"userId\": 1,\n"
                + "  \"partnerId\": 1,\n"
                + "  \"role\": \"ADMIN\"\n"
                + "}";
        var response = grantRole(1, 1, Role.ADMIN, 10000);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Успешная выдача роли новому пользователю")
    @DbUnitDataSet(after = "UserPartnerRoleControllerTest.grantRoleToNewUser.after.csv")
    void grantRoleToNewUser() {
        var expected = ""
                + "{\n"
                + "  \"id\": \"${json-unit.ignore}\",\n"
                + "  \"userId\": 11,\n"
                + "  \"partnerId\": 1,\n"
                + "  \"role\": \"ADMIN\"\n"
                + "}";
        var response = grantRole(11, 1, Role.ADMIN, 10000);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Попытка выдать уже существующую роль")
    void grantExistedRole() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> grantRole(1, 1, Role.ANALYTICS, 10000)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        var expected = ""
                + "{\n"
                + "     \"code\": \"ENTITY_ALREADY_EXISTS\",\n"
                + "     \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Попытка выдать пользователю роль в ещё одном магазине")
    void grantRoleInAnotherShop() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> grantRole(1, 4, Role.ANALYTICS, 10000)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        var expected = ""
                + "{\n"
                + "     \"code\":\"USER_LINKED_TO_ANOTHER_SHOP\",\n"
                + "     \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Попытка выдать пользователю роль в ещё одном магазине (предыдущая роль неактивна)")
    @DbUnitDataSet(after = "UserPartnerRoleControllerTest.grantRoleInAnotherShopSuccess.after.csv")
    void grantRoleInAnotherShopSuccess() {
        var expected = ""
                + "{\n"
                + "  \"id\": \"${json-unit.ignore}\",\n"
                + "  \"userId\": 2,\n"
                + "  \"partnerId\": 4,\n"
                + "  \"role\": \"ANALYTICS\"\n"
                + "}";
        var response = grantRole(2, 4, Role.ANALYTICS, 10000);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Выдача вендорскому пользователю магазинной роли")
    @DbUnitDataSet(after = "UserPartnerRoleControllerTest.grantShopRoleToVendorUser.after.csv")
    void grantShopRoleToVendorUser() {
        var expected = ""
                + "{\n"
                + "  \"id\": \"${json-unit.ignore}\",\n"
                + "  \"userId\": 4,\n"
                + "  \"partnerId\": 4,\n"
                + "  \"role\": \"ADMIN\"\n"
                + "}";
        var response = grantRole(4, 4, Role.ADMIN, 10000);
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Попытка выдать пользователю роль для несуществующего партнёра")
    void grantRoleUnknownPartner() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> grantRole(1, 10, Role.ANALYTICS, 10001)
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        var expected = ""
                + "{\n"
                + "     \"code\":\"ENTITY_NOT_FOUND\",\n"
                + "     \"message\": \"${json-unit.ignore}\",\n"
                + "     \"entityId\": 10,\n"
                + "     \"entityType\": \"PARTNER\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Успешный отзыв роли")
    @DbUnitDataSet(after = "UserPartnerRoleControllerTest.revokeRole.after.csv")
    void revokeRole() {
        revokeRole(1000, 1);
    }

    @Test
    @DisplayName("Попытка повторно отозвать роль")
    void revokeInactiveRole() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> revokeRole(1001, 10001)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        var expected = ""
                + "{\n"
                + "     \"code\":\"ROLE_ALREADY_REVOKED\",\n"
                + "     \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Попытка отозвать несуществующую роль")
    void revokeUnknownRole() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> revokeRole(777, 10001)
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        var expected = ""
                + "{\n"
                + "     \"code\":\"ENTITY_NOT_FOUND\",\n"
                + "     \"message\": \"${json-unit.ignore}\",\n"
                + "     \"entityId\": 777,\n"
                + "     \"entityType\": \"USER_PARTNER_ROLE\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Отзыв последней админской роли у партнёра")
    void revokeLastAdminRole() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> revokeRole(1002, 1)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        var expected = ""
                + "{\n"
                + "     \"code\":\"REMOVE_LAST_ADMINS\",\n"
                + "     \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Пользователь не является пользователем партнера, у которого отзывает роль.")
    void revokeAnotherPartnersRole() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> revokeRole(1000, 5)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        var expected = ""
                + "{\n"
                + "     \"code\":\"ACTION_IS_FORBIDDEN\",\n"
                + "     \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Успешный отзыв админской роли")
    @DbUnitDataSet(after = "UserPartnerRoleControllerTest.revokeAdminRoleSuccess.after.csv")
    void revokeAdminRoleSuccess() {
        revokeRole(1004, 5);
    }

    @Test
    @DisplayName("Получение всех активных ролей для партнёра")
    void getPartnerRoles() {
        var response = partnerRoles(2);
        var expected = loadFromFile("UserPartnerRoleControllerTest.getPartnerRoles.expected.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(expected, response);
    }

    @Test
    @DisplayName("Получение пользователей партнёра")
    void getPartnerUsers() {
        var response = partnerUsers(4);
        var expected = loadFromFile("UserPartnerRoleControllerTest.getPartnerUsers.expected.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(expected, response);
    }

    private String grantRole(long uid, long partnerId, Role role, long managerUid) {
        String body = ""
                + "{\n"
                + "  \"userId\": " + uid + ",\n"
                + "  \"partnerId\": " + partnerId + ",\n"
                + "  \"role\": \"" + role + "\"\n"
                + "}";
        return FunctionalTestHelper.postForJson(rolesUrl(managerUid), body);
    }

    private String rolesUrl(long managerUid) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path(ROLES_URL)
                .queryParam("managerUid", managerUid)
                .toUriString();
    }

    private void revokeRole(long roleId, long managerUid) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path(ROLES_URL)
                .path("/{roleId}")
                .queryParam("managerUid", managerUid)
                .buildAndExpand(roleId)
                .toUriString();
        FunctionalTestHelper.delete(url);
    }

    private String partnerRoles(long partnerId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path(ROLES_URL)
                .queryParam("partnerId", partnerId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String partnerUsers(long partnerId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path(ROLES_URL + "/partner/{partnerId}")
                .buildAndExpand(partnerId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}
