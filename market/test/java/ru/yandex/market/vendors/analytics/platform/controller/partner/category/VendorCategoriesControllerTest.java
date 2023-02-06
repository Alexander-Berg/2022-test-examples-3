package ru.yandex.market.vendors.analytics.platform.controller.partner.category;

import com.google.common.collect.ImmutableMap;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests for {@link VendorCategoriesController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "PartnerCategoriesControllerTest.csv")
class VendorCategoriesControllerTest extends FunctionalTest {

    private static final long PARTNER_ID = 1L;

    @Test
    @DisplayName("Получение информации о категориях партнера")
    @DbUnitDataSet
    void getPartnerInfo() {
        var expected = ""
                + "{\n"
                + "  \"partner\": {\n"
                + "    \"id\": 1,\n"
                + "    \"type\": \"VENDOR\"\n"
                + "  },\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 10,\n"
                + "      \"name\": \"10_имя\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 15,\n"
                + "      \"name\": \"15_имя\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 20,\n"
                + "      \"name\": \"20_имя\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        var actual = getPartnerCategories(PARTNER_ID);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Сброс текущих категорий партнера и сохранение новых")
    @DbUnitDataSet(after = "resetPartner.csv")
    void resetPartner() {
        String body = "{\"hids\" : [100, 2000]}";
        resetPartner(3L, body);
    }

    @Test
    @DisplayName("Сброс текущих категорий партнера")
    @DbUnitDataSet(after = "blankPartner.csv")
    void resetPartnerToBlank() {
        String body = "{\"hids\" : []}";
        resetPartner(3L, body);
    }

    @Test
    @DisplayName("Партнер не найден")
    @DbUnitDataSet
    void getPartnerInfoNotFound() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getPartnerCategories(100L)
        );
        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
        String expected = "" +
                "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"Entity PARTNER not found by id: 100\",\n" +
                "   \"entityId\":100,\n" +
                "   \"entityType\":\"PARTNER\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DbUnitDataSet(after = "deleteCategory.csv")
    @DisplayName("Удаление категории партнера")
    void deleteCategory() {
        deleteCategory(PARTNER_ID, 15L);
    }

    @Test
    @DbUnitDataSet(after = "addCategory.csv")
    @DisplayName("Добавление категории партнера")
    void addCategory() {
        var expected = ""
                + "{\n"
                + "  \"partner\": {\n"
                + "    \"id\": 1,\n"
                + "    \"type\": \"VENDOR\"\n"
                + "  },\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 10,\n"
                + "      \"name\": \"10_имя\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 15,\n"
                + "      \"name\": \"15_имя\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 20,\n"
                + "      \"name\": \"20_имя\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 100,\n"
                + "      \"name\": \"100_имя\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        var actual = addCategory(PARTNER_ID, 100L);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    private String getAnalyticsCategoriesUrl(long partnerId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/partners/{partnerId}/categories")
                .buildAndExpand(partnerId)
                .toUriString();
    }

    private String getPartnerCategories(long partnerId) {
        String analyticsUrl = getAnalyticsCategoriesUrl(partnerId);
        return FunctionalTestHelper.get(analyticsUrl).getBody();
    }

    private void resetPartner(long partnerId, String body) {
        String analyticsUrl = getAnalyticsCategoriesUrl(partnerId);
        FunctionalTestHelper.putForJson(analyticsUrl, body);
    }

    private String addCategory(long partnerId, long hid) {
        String analyticsUrl = getAnalyticsCategoriesUrl(partnerId) + "?hid={hid}";
        return FunctionalTestHelper.post(analyticsUrl, null, hid).getBody();
    }

    private void deleteCategory(long partnerId, long hid) {
        String analyticsUrl = getAnalyticsCategoriesUrl(partnerId) + "?hid={hid}";
        FunctionalTestHelper.delete(analyticsUrl, ImmutableMap.of("hid", hid));
    }
}
