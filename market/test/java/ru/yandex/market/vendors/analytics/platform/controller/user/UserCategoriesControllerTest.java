package ru.yandex.market.vendors.analytics.platform.controller.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * @author ogonek
 */
@DbUnitDataSet(before = "UserCategoriesControllerTest.before.csv")
public class UserCategoriesControllerTest extends FunctionalTest {

    private static final long UID = 123L;

    @Test
    @DisplayName("Получение всех доступных категорий")
    void getAvailableCategories() {
        String expected = ""
                + "[\n"
                + "  {\n"
                + "    \"hid\": 31,\n"
                + "    \"name\": \"Автомобили\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"hid\": 45,\n"
                + "    \"name\": \"Двигатели\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"hid\": 39,\n"
                + "    \"name\": \"Железные троны\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"hid\": 37,\n"
                + "    \"name\": \"Электроника\"\n"
                + "  }\n"
                + "]";
        String response = getAllCategories(UID);

        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Получение всех доступных категорий при отсутствии таковых")
    void getEmptyAvailableCategories() {
        String response = getAllCategories(125);
        String expected = "[]";

        JsonTestUtil.assertEquals(
                expected,
                response
        );
    }

    @Test
    @DisplayName("Получение всех доступных категорий по части имени")
    void getAvailableCategoriesByPartName() {
        String response = getAllCategoriesByCategoryPartName(UID, "ТРон");
        String expected = "[\n"
                + "  {\n"
                + "    \"hid\": 39,\n"
                + "    \"name\": \"Железные троны\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"hid\": 37,\n"
                + "    \"name\": \"Электроника\"\n"
                + "  }\n"
                + "]";

        JsonTestUtil.assertEquals(expected, response);
    }

    private String categoriesUrl(long userId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/categories")
                .buildAndExpand(userId)
                .toUriString();
    }

    private String getAllCategories(long userId) {
        String categoriesUrl = categoriesUrl(userId);
        return FunctionalTestHelper.get(categoriesUrl).getBody();
    }

    private String getAllCategoriesByCategoryPartName(long userId, String partName) {
        String categoriesUrl = categoriesUrl(userId) + "?partName=" + partName;
        return FunctionalTestHelper.get(categoriesUrl).getBody();
    }

}
