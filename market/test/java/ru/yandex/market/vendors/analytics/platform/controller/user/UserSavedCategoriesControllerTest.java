package ru.yandex.market.vendors.analytics.platform.controller.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "UserSavedCategoriesControllerTest.before.csv")
public class UserSavedCategoriesControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Сохранить категории первый раз")
    @DbUnitDataSet(after = "UserSavedCategoriesControllerTest.saveCategoriesFirstTime.after.csv")
    void saveCategoriesFirstTime() {
        var body = """
                {
                "categoryTariffs": [{
                   "hid": 2,
                   "tariffType": "FULL",
                   "monthCount": 1
                },
                {
                   "hid": 4,
                   "tariffType": "BASIC",
                   "monthCount": 9
                }]
                }
                """;
        var response = saveCategories(3, 1000L, body);
        assertJsonEquals(body, response);
    }

    @Test
    @DisplayName("Сохранить категории повторно")
    @DbUnitDataSet(after = "UserSavedCategoriesControllerTest.saveCategories.after.csv")
    void saveCategories() {
        var body = "{"
                   + "\"categoryTariffs\": []"
                   + "}";
        var response = saveCategories(1, 1000L, body);
        assertJsonEquals(body, response);
    }

    @Test
    @DisplayName("Получить сохраненные категории")
    void getCategories() {
        var response = getCategories(1, 1000L);
        var expected = """
                {
                "categoryTariffs": [{
                   "hid": 1,
                   "tariffType": "FULL",
                   "monthCount": 1
                },
                {
                   "hid": 3,
                   "tariffType": "BASIC",
                   "monthCount": 9
                }]
                }
                """;
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получить сохраненные категории: нет записи")
    void getCategoriesNull() {
        var response = getCategories(3, 1000L);
        var expected = "{\"categoryTariffs\": []}";
        assertJsonEquals(expected, response);
    }

    private String saveCategories(long userId, long vendorId, String body) {
        var url = url(userId, vendorId);
        return FunctionalTestHelper.postForJson(url, body);
    }

    private String getCategories(long userId, long vendorId) {
        var url = url(userId, vendorId);
        return FunctionalTestHelper.get(url).getBody();
    }

    public String url(long userId, long vendorId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("users", "{userId}", "offer", "categories")
                .queryParam("vendorId", vendorId)
                .buildAndExpand(userId)
                .toUriString();
    }
}
