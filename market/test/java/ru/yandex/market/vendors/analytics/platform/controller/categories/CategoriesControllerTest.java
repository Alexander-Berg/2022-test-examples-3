package ru.yandex.market.vendors.analytics.platform.controller.categories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.category.CategoriesController;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты контроллера {@link CategoriesController}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "CategoriesControllerTest.before.csv")
public class CategoriesControllerTest extends FunctionalTest {

    /**
     * Simple test for {@link CategoriesController#getCategoryInfo(long, LanguageDTO)}: case all correct.
     */
    @Test
    @DisplayName("Получение информации о категории")
    void getCategoryInfoOkTest() {
        String expected = ""
                + "{\n"
                + "  \"hid\": 33,\n"
                + "  \"name\": \"Тойота\",\n"
                + "  \"type\": \"GURU\"\n"
                + "}";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/categories/33");
        assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Для категории неизвестен тип")
    void getCategoryInfoNullType() {
        String expected = ""
                + "{\n"
                + "  \"hid\": 32,\n"
                + "  \"name\": \"Электроника\",\n"
                + "  \"type\": null\n"
                + "}";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/categories/32");
        assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Неверный hid категории")
    void getCategoryInfoFailTest() {
        long unknownHid = -1;
        assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl() + "/categories/" + unknownHid),
                "400 Bad Request"
        );
    }

    /**
     * Тест нормальной работы ручки {@link CategoriesController#getCategoriesTree(String, Integer, LanguageDTO)}
     * по названию.
     */
    @Test
    @DisplayName("Поиск категорий по части названия")
    void getCategoriesTreeByName() {
        String expected = ""
                + "[\n"
                + "  {\n"
                + "    \"hid\": 37,\n"
                + "    \"name\": \"Двигатели\",\n"
                + "    \"branch\": [\n"
                + "      \"Двигатели\",\n"
                + "      \"Автомобили\"\n"
                + "    ]\n"
                + "  },\n"
                + "  {\n"
                + "    \"hid\": 32,\n"
                + "    \"name\": \"Электроника\",\n"
                + "    \"branch\": [\n"
                + "      \"Электроника\"\n"
                + "    ]\n"
                + "  }\n"
                + "]";

        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl() + "/categories/tree?partName=И");
        assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    /**
     * Тест нормальной работы ручки {@link CategoriesController#getCategoriesTree(String, Integer, LanguageDTO)} по hid.
     */
    @Test
    @DisplayName("Поиск категорий по части hid")
    void getCategoriesTreeByHid() {
        String expected = ""
                + "[\n"
                + "  {\n"
                + "    \"hid\": 37,\n"
                + "    \"name\": \"Двигатели\",\n"
                + "    \"branch\": [\n"
                + "      \"Двигатели\",\n"
                + "      \"Автомобили\"\n"
                + "    ]\n"
                + "  }\n"
                + "]";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/categories/tree?partName=37");
        assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    /**
     * Тест работы ручки {@link CategoriesController#getCategoriesTree(String, Integer, LanguageDTO)}
     * при отстутствии подходящих категорий.
     */
    @Test
    void getEmptyCategoriesTreeTest() {
        String expected = "[]";

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/categories/tree?partName=fdgdf");
        assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    /**
     * Test for {@link CategoriesController#getCategorySiblings(long, LanguageDTO)}.
     */
    @Test
    @DisplayName("Получение сиблингов")
    void getCategorySiblings() {
        String expected = ""
                + "{\n"
                + "  \"parent\": {\n"
                + "    \"hid\": 31,\n"
                + "    \"name\": \"Автомобили\"\n"
                + "  },\n"
                + "  \"siblings\": [\n"
                + "    {\n"
                + "      \"hid\": 37,\n"
                + "      \"name\": \"Двигатели\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 33,\n"
                + "      \"name\": \"Тойота\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl() + "/categories/siblings?hid=33");
        assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Получение полной информации по двум категориям")
    void categoriesFullInfo() {
        String expected = loadFromFile("CategoriesFullInfo.response.json");
        String response = FunctionalTestHelper.get(baseUrl() + "/categories?hid=32,33").getBody();
        assertNotNull(response);
        JsonTestUtil.assertEquals(expected, response);
    }

}
