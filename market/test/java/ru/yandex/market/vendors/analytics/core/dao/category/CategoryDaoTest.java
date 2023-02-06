package ru.yandex.market.vendors.analytics.core.dao.category;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.model.categories.Category;
import ru.yandex.market.vendors.analytics.core.model.categories.CategoryInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Функциональные тесты для класса {@link CategoryDao}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "CategoryDaoTest.before.csv")
public class CategoryDaoTest extends FunctionalTest {

    @Autowired
    private CategoryDao categoryDao;

    @Test
    @DisplayName("Получение информации о листовых категориях по части имени категории")
    void getSheetCategoriesRu() {
        var expected = List.of(
                new CategoryInfo(37L, 31L, "Двигатели", "Engines", "guru"),
                new CategoryInfo(32L, 1L, "Электроника", "Electronics", null)
        );
        List<CategoryInfo> actual = categoryDao.getSheetCategories("И", 10);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получение информации о листовых категориях по части имени категории на английском")
    void getSheetCategoryEn() {
        var expected = List.of(
                new CategoryInfo(37, 31, "Двигатели", "Engines", "guru"),
                new CategoryInfo(39, 1, "Мегатрон", "Megatron", null),
                new CategoryInfo(32, 1, "Электроника", "Electronics", null),
                new CategoryInfo(38, 1, "Железные троны", "Iron Thrones", null)
        );
        List<CategoryInfo> actual = categoryDao.getSheetCategories("e", 10);
        //На OSX сортировка не отрабатывает
        MatcherAssert.assertThat(
                actual,
                Matchers.containsInAnyOrder(
                        expected.toArray(new CategoryInfo[expected.size()])
                )
        );
    }

    @Test
    @DisplayName("Получение информации о листовых категориях по части hid категории")
    void getSheetCategoriesHid() {
        var expected = List.of(new CategoryInfo(37L, 31L, "Двигатели", "Engines", "guru"));
        List<CategoryInfo> actual = categoryDao.getSheetCategories("37", 10);
        assertEquals(expected, actual);
    }

    @Test
    void getParentTest() {
        Category expected = new Category(1, "Все", "All");
        Optional<Category> actual = categoryDao.getParent(32);
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    void getParentTestNotFound() {
        Optional<Category> parent = categoryDao.getParent(1);
        assertFalse(parent.isPresent());
    }

    @Test
    @DisplayName("Получение листовых категорий по списку hid'ов")
    void getSheetCategoriesByIdTest() {
        var expected = List.of(new CategoryInfo(32L, 1L, "Электроника", "Electronics", null));
        List<CategoryInfo> actual = categoryDao.getSheetCategories(Arrays.asList(31L, 32L, 40L));
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получение списка категорий по части названия")
    void getCategoriesByPartName() {
        var expected = List.of(
                new Category(38, "Железные троны","Iron Thrones"),
                new Category(32, "Электроника", "Electronics")
        );
        List<Category> actual = categoryDao.getCategoriesByHidsAndPartName(List.of(32L, 37L, 38L), "Трон");
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получение списка категорий по части названия на английском")
    void getCategoriesByPartEnglishName() {
        var expected = List.of(
                new Category(38, "Железные троны","Iron Thrones"),
                new Category(39, "Мегатрон","Megatron"),
                new Category(32, "Электроника","Electronics")
        );
        List<Category> actual = categoryDao.getCategoriesByHidsAndPartName(List.of(32L, 37L, 38L, 39L), "ON");
        //На OSX сортировка не отрабатывает
        MatcherAssert.assertThat(
                actual,
                Matchers.containsInAnyOrder(
                        expected.toArray(new Category[expected.size()])
                )
        );
    }
}
