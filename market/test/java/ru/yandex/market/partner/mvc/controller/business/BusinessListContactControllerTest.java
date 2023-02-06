package ru.yandex.market.partner.mvc.controller.business;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static ru.yandex.market.common.test.util.JsonTestUtil.parseJson;

/**
 * Проверяем получение контактов в {@link BusinessContactController#getContacts}.
 */
@DbUnitDataSet(before = "contact/contactControllerTest.before.csv")
class BusinessListContactControllerTest extends FunctionalTest {

    private static Stream<Arguments> args() {
        return Stream.of(
                //запрос по бизнесу один контакт
                Arguments.of(10, "{}", "contact/oneContactExpected.json"),
                //запрос по бизнесу shop_admin
                Arguments.of(10, "{\"innerRoles\":[1]}", "contact/oneContactExpected.json"),
                //запрос по бизнесу не контакта
                Arguments.of(11, "{}", "contact/emptyContactsExpected.json"),
                //запрос по бизнесу несколько контактов
                Arguments.of(12, "{}", "contact/twoContactsExpected.json"),
                //запрос по бизнесу несколько контактов с фильтром email
                Arguments.of(12, "{\"searchString\":\"spb\"}", "contact/filteredContactsExpected.json"),
                //запрос по бизнесу несколько контактов с фильтром campaignIds
                Arguments.of(13, "{\"campaignIds\":[301]}", "contact/manyContactsWithFilter.json"),
                //запрос по бизнесу с фильтром campaignIds и только админов
                Arguments.of(13, "{\"campaignIds\":[301],\"innerRoles\":[6]}", "contact" +
                        "/manyContactsWithCampaignAndRoleFilter.json"),
                //запрос по бизнесу с фильтром campaignIds и роли
                Arguments.of(13, "{\"campaignIds\":[301],\"innerRoles\":[4]}",
                        "contact/ManyContactsInviteWithFilterRole.json"),
                //запрос по бизнесу несколько контактов с фильтром campaignIds
                Arguments.of(13, "{\"campaignIds\":[301],\"innerRoles\":[3]}",
                        "contact/manyContactsWithFilterRole.json"),
                //запрос по бизнесу несколько контактов с фильтром по ролям
                Arguments.of(14, "{\"innerRoles\":[4]}", "contact/filterByRole.json"),
                //запрос по бизнесу контакта, у которого есть роль в бизнесе, но нет contact_link на сами кампании.
                //Подробнее MBI-50467
                Arguments.of(16, "{\"innerRoles\":[5]}", "contact/filterBusinessContactWithoutLinkByRole.json"),
                //запрос по бизнесу контакта, который является приглашением
                Arguments.of(17, "{}", "contact/oneInviteExpected.json"),
                //запрос по бизнесу контакта, который является приглашением с фильтром по кампании
                Arguments.of(17, "{\"campaignIds\":[307]}", "contact/oneInviteExpected.json"),
                //запрос по бизнесу контакта, который является приглашением с фильтром по кампании и роли админа
                Arguments.of(17, "{\"campaignIds\":[307],\"innerRoles\":[6]}", "contact/oneInviteExpected.json")
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void checklistContact(long businessId, String body, String expectedPath) {
        ResponseEntity<String> response = FunctionalTestHelper.post(getUrl(), body, businessId);
        assertResult(response, expectedPath);
        String pagingToken = getPagingToken(response);
        response = FunctionalTestHelper.post(getUrl() +
                "&page_token={pager}", body, businessId, pagingToken);
        JsonTestUtil.assertEquals(response, "{\"contacts\":[],\"paging\":{}}");
    }

    private String getUrl() {
        return baseUrl + "/businesses/{businessId}/contacts/list?with_invites=true";
    }

    @Test
    void checklistManyContacts() {
        long businessId = 13;
        ResponseEntity<String> response =
                FunctionalTestHelper.post(getUrl() + "&limit=8", "{}", businessId);
        assertResult(response, "contact/manyContacts.json");
        var pagingToken = getPagingToken(response);
        response = FunctionalTestHelper.post(getUrl() + "&limit=10&page_token=" + pagingToken,
                "{}", businessId);
        assertResult(response, "contact/manyContactsSecondPage.json");

        pagingToken = getPagingToken(response);
        response = FunctionalTestHelper.post(getUrl() + "&limit=10&page_token=" + pagingToken,
                "{}", businessId);
        JsonTestUtil.assertEquals(response, "{\"contacts\":[],\"paging\":{}}");

    }

    @ParameterizedTest
    @MethodSource("testCountContactsArgs")
    void testCountContact(long businessId, String body, int expectedCount) {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/businesses/{businessId}/contacts/list/count?with_invites=true", body, businessId);
        JsonTestUtil.assertEquals(response, "{\"count\": " + expectedCount + "}");
    }

    private static Stream<Arguments> testCountContactsArgs() {
        return Stream.of(
                //запрос по бизнесу один контакт
                Arguments.of(10, "{}", 1),
                //запрос по бизнесу shop_admin
                Arguments.of(10, "{\"innerRoles\":[1]}", 1),
                //запрос по бизнесу не контакта
                Arguments.of(11, "{}", 0),
                //запрос по бизнесу несколько контактов
                Arguments.of(12, "{}", 2),
                //запрос по бизнесу несколько контактов с фильтром email
                Arguments.of(12, "{\"searchString\":\"spb\"}", 1),
                //запрос по бизнесу несколько контактов с фильтром campaignIds
                Arguments.of(13, "{\"campaignIds\":[301]}", 12),
                //запрос по бизнесу с фильтром campaignIds и только админов
                Arguments.of(13, "{\"campaignIds\":[301],\"innerRoles\":[6]}", 9),
                //запрос по бизнесу с фильтром campaignIds и роли
                Arguments.of(13, "{\"campaignIds\":[301],\"innerRoles\":[4]}", 1),
                //запрос по бизнесу несколько контактов с фильтром campaignIds
                Arguments.of(13, "{\"campaignIds\":[301],\"innerRoles\":[3]}", 1),
                //запрос по бизнесу несколько контактов с фильтром по ролям
                Arguments.of(14, "{\"innerRoles\":[4]}", 1),
                //запрос по бизнесу контакта, который является приглашением
                Arguments.of(17, "{}", 1),
                //запрос по бизнесу контакта, который является приглашением с фильтром по кампании
                Arguments.of(17, "{\"campaignIds\":[307]}", 1),
                //запрос по бизнесу контакта, который является приглашением с фильтром по кампании и роли админа
                Arguments.of(17, "{\"campaignIds\":[307],\"innerRoles\":[6]}", 1)
        );
    }


    private void assertResult(ResponseEntity<String> response, String expectedPath) {
        try (var resource = getClass().getResourceAsStream(expectedPath)) {
            JSONAssert.assertEquals(IOUtils.toString(resource, StandardCharsets.UTF_8),
                    parseJson(response.getBody()).getAsJsonObject().get("result").toString(),
                    JSONCompareMode.NON_EXTENSIBLE);
        } catch (IOException ignored) {
        }
    }

    @Nullable
    private String getPagingToken(ResponseEntity<String> response) {
        var jsonPagingObject = parseJson(response.getBody())
                .getAsJsonObject().get("result")
                .getAsJsonObject().get("paging")
                .getAsJsonObject();
        return jsonPagingObject.size() == 0 ? null : jsonPagingObject.get("nextPageToken").getAsString();
    }
}
