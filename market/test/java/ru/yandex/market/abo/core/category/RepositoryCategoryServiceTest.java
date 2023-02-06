package ru.yandex.market.abo.core.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author imelnikov
 * @since 30.11.2020
 */
public class RepositoryCategoryServiceTest extends EmptyTest {

    @Autowired
    RepositoryCategoryService categoryService;

    @Test
    public void updateCategory() {
        categoryService.updateCategoryInfo(-1, "Root", null);
        categoryService.updateCategoryInfo(-2, "Подвес", -1);
        categoryService.updateCategoryInfo(-3, "Подвес", -1);
        categoryService.getCategoryWithParentById(-3);
    }
}
