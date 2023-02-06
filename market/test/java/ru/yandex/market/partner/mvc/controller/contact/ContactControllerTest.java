package ru.yandex.market.partner.mvc.controller.contact;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * Тесты для {@link ContactController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "ContactControllerTest.before.csv")
public class ContactControllerTest extends FunctionalTest {

    private static Stream<Arguments> getEuidArgs() {
        return Stream.of(
                Arguments.of(4000L, null, 40L),// пришли с бизнесом
                Arguments.of(null, 41L, 40L),  // пришли с id кампании
                Arguments.of(5000L, 51L, 50L), // пришли зачем-то с id бизнеса и id кампании
                Arguments.of(null, 61L, 60L),  // пришли с id кампании TPL
                Arguments.of(4000L, 52L, 40L), // пришли зачем-то с id бизнеса и id кампании не этого бизнеса
                Arguments.of(4000L, 61L, 40L)  // пришли зачем-то с id бизнеса и id кампании-TPL не связанной с бизнесом
        );
    }

    /**
     * Проверяет получение контакта euid'a.
     *
     * @see ContactController#getCurrentContact
     */
    @Test
    void testGetCurrentContact() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "contacts/current?_user_id=10");
        assertEquals(response, "{\"contactId\": 1}");
    }

    /**
     * Проверяет 404 для несуществующего пользователя.
     *
     * @see ContactController#getCurrentContact
     */
    @Test
    void testGetCurrentContact404() {
        HttpClientErrorException.NotFound exception = assertThrows(HttpClientErrorException.NotFound.class,
                () -> FunctionalTestHelper.get(baseUrl + "contacts/current?_user_id=999"));
        assertThat(exception.getResponseBodyAsString(), containsString("No contact for euid 999"));
    }

    /**
     * Проверяет обновление email'ов.
     *
     * @see ContactController#updateEmails
     */
    @Test
    @DbUnitDataSet(after = "ContactControllerTest.updateEmails.after.csv")
    void testUpdateEmails() {
        FunctionalTestHelper.put(baseUrl + "contacts/emails?_user_id=10&email=spbtester@yandex.ru&email=msk@yandex.ru",
                null);
    }

    /**
     * Проверяет обновление email'ов.
     *
     * @see ContactController#updateEmails
     */
    @Test
    @DbUnitDataSet(before = "ContactControllerTest.updateEmails.before.csv",
            after = "ContactControllerTest.updateEmails.after.csv")
    void testUpdateEmailsWithDuplicate() {
        FunctionalTestHelper.put(baseUrl + "contacts/emails?_user_id=10&email=spbtester@yandex.ru&email=msk@yandex.ru",
                null);
    }

    @Test
    @DisplayName("Проверка, что берется контакт по campaignId для contacts/list")
    void getContactList() {
        var result = FunctionalTestHelper.get(baseUrl + "/contacts/list?has_user_id=true&_user_id" +
                "=10&campaignId=1");
        JsonTestUtil.assertEquals(result, getClass(), "get_contact_list.json");
    }

    @Test
    @DisplayName("Проверка, что достаются только доступные кампаниии")
    void getContactListAllCampaigns() {
        var result = FunctionalTestHelper.get(baseUrl + "/contacts/list?has_user_id=true&_user_id" +
                "=10");
        JsonTestUtil.assertEquals(result, getClass(), "get_contact_list.json");
    }

    @Test
    @DisplayName("Проверка, innerRoles для contacts/list")
    void testContactInnerRoles() {
        resultExists(baseUrl + "/contacts/list?euid=&format=json&has_user_id=true&_user_id=10&inner_roles" +
                "=1,3");
        resultNotExists(baseUrl + "/contacts/list?euid=&format=json&has_user_id=true&_user_id=10" +
                "&inner_roles=4");
    }

    @Test
    @DisplayName("Проверка, nameOrEmail для contacts/list")
    void testContactNameOrEmail() {
        resultExists(baseUrl + "/contacts/list?euid=&format=json&has_user_id=true&_user_id=10&name_or_email=vasya");
        resultNotExists(baseUrl + "/contacts/list?euid=&format=json&has_user_id=true&_user_id=10&name_or_email=jordan");
        resultExists(baseUrl + "/contacts/list?euid=&format=json&has_user_id=true&_user_id=10&name_or_email=spb");
        resultNotExists(baseUrl + "/contacts/list?euid=&format=json&has_user_id=true&_user_id=10&name_or_email=NOTspb");
    }

    @ParameterizedTest
    @MethodSource("getEuidArgs")
    @DisplayName("Проверка получения владельца партнера")
    void getEuid(Long businessId, Long campaignId, Long expectedOwnerUid) {
        ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/contacts/euid?businessId=" +
                (businessId == null ? "" : businessId) + "&campaignId=" + (campaignId == null ? "" : campaignId));
        JsonTestUtil.assertEquals(result, expectedOwnerUid.toString());
    }

    private void resultExists(String url) {
        var result = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(result, getClass(), "get_contact_list.json");
    }

    private void resultNotExists(String url) {
        var result = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(result, getClass(), "empty_contact_list.json");
    }
}
