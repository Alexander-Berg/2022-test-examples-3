package ru.yandex.market.mbi.tariffs.service;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.model.Category;
import ru.yandex.market.mbi.tariffs.service.category.CategoryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.mbi.tariffs.matcher.CategoryMatcher.hasAllFields;

/**
 * Тест для {@link ru.yandex.market.mbi.tariffs.service.category.CategoryService}
 */
@ParametersAreNonnullByDefault
public class CategoryServiceTest extends FunctionalTest {
    @Autowired
    private CategoryService categoryService;

    @Test
    void testCategoryExists() {
        assertTrue(categoryService.categoryExists(90401L)); //Все товары
        assertTrue(categoryService.categoryExists(198119L)); //Электроника
        assertFalse(categoryService.categoryExists(39L)); //Неизвестная категория
    }

    @Test
    void testGetAllCategories() {
        List<Category> all = categoryService.getAll();
        assertThat(all, containsInAnyOrder(
                hasAllFields(90401, null, "Все товары"),
                hasAllFields(198119, 90401L, "Электроника"),
                hasAllFields(90509, 90401L, "Товары для красоты"),
                hasAllFields(90666, 90401L, "Товары для дома"),
                hasAllFields(90719, 90401L, "Дача, сад и огород"),
                hasAllFields(1981180, 90401L, "Бытовая техника"),
                hasAllFields(91009, 90401L, "Компьютерная техника"),
                hasAllFields(90607, 198119L, "Фото и видеокамеры")
        ));
    }

    @Test
    void testGetAllCategoriesIn() {
        List<Category> all = categoryService.getAllIn(Arrays.asList(198119L, 90666L));
        assertEquals(2, all.size());
        assertThat(all, containsInAnyOrder(
                hasAllFields(198119L, 90401L, "Электроника"),
                hasAllFields(90666L, 90401L, "Товары для дома")
        ));
    }

    @Test
    void testGetAllStartsWith() {
        List<Category> allStartsWith = categoryService.getAllStartsWith("90");
        assertThat(allStartsWith, contains(
                hasAllFields(90401, null, "Все товары"),
                hasAllFields(90509, 90401L, "Товары для красоты"),
                hasAllFields(90607, 198119L, "Фото и видеокамеры"),
                hasAllFields(90666, 90401L, "Товары для дома"),
                hasAllFields(90719, 90401L, "Дача, сад и огород")
        ));
    }

    @Test
    @DbUnitDataSet(after = "category/saveCategories.after.csv")
    void testSaveCategories() {
        List<Category> newCategories = List.of(
                new Category().hyperId(1L).name("root"),
                new Category().hyperId(2L).parentId(1L).name("root -> 2nd"),
                new Category().hyperId(3L).parentId(1L).name("root -> 3rd"),
                new Category().hyperId(4L).parentId(2L).name("root -> 2nd -> 4th")
        );

        categoryService.saveCategories(newCategories);
    }

    @Test
    @DbUnitDataSet(after = "classpath:ru/yandex/market/mbi/tariffs/categories.csv")
    void testSaveCategoriesWithException() {
        List<Category> newCategories = List.of(
                new Category().hyperId(1L).name("root"),
                new Category().hyperId(1L).parentId(1L).name("root -> 2nd")
        );

        assertThrows(Exception.class, () -> categoryService.saveCategories(newCategories));
    }
}
