package ru.yandex.market.core.category.db;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.category.model.Category;
import ru.yandex.market.core.category.model.CategoryType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

@DbUnitDataSet(before = "DbCategoryDaoTest.before.csv")
public class DbCategoryDaoTest extends FunctionalTest {

    private static final Category[] EXPECTED = new Category[]{
            new Category.Builder()
                    .setNoSearch(false)
                    .setNotUsed(false)
                    .setCategory("Все товары")
                    .setHyperId(10L)
                    .setParentId(null)
                    .setId(100L)
                    .setType(CategoryType.UNDEFINED)
                    .setAcceptGoodContent(false)
                    .setAcceptWhiteContent(false)
                    .setPublished(true)
                    .build(),
            new Category.Builder()
                    .setNoSearch(true)
                    .setNotUsed(true)
                    .setCategory("Электроника")
                    .setHyperId(20L)
                    .setParentId(100L)
                    .setId(200L)
                    .setType(CategoryType.UNDEFINED)
                    .setAcceptGoodContent(false)
                    .setAcceptWhiteContent(false)
                    .setPublished(true)
                    .build(),
            new Category.Builder()
                    .setNoSearch(false)
                    .setNotUsed(true)
                    .setCategory("Мобильные телефоны")
                    .setHyperId(30L)
                    .setParentId(200L)
                    .setId(300L)
                    .setType(CategoryType.UNDEFINED)
                    .setAcceptGoodContent(false)
                    .setAcceptWhiteContent(false)
                    .setPublished(false)
                    .build()
    };

    @Autowired
    private DbCategoryDao dao;

    @Test
    public void testGetAll() {
        List<Category> actual = dao.getAll();
        assertThat(actual, containsInAnyOrder(EXPECTED));
    }

}
