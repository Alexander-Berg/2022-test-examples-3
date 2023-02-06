package ru.yandex.market.core.category;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.core.category.db.DbCategoryDao;
import ru.yandex.market.core.category.model.Category;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class CategoryWalkerTest {

    private static final ImmutableList<Category> EXPECTED = ImmutableList.of(
            new Category.Builder()
                    .setNoSearch(false)
                    .setNotUsed(false)
                    .setCategory("Все товары")
                    .setHyperId(100L)
                    .setParentId(null)
                    .setId(1L)
                    .build(),
            new Category.Builder()
                    .setNoSearch(true)
                    .setNotUsed(true)
                    .setCategory("Электроника")
                    .setHyperId(200L)
                    .setParentId(1L)
                    .setId(2L)
                    .build(),
            new Category.Builder()
                    .setNoSearch(false)
                    .setNotUsed(true)
                    .setCategory("Мобильные телефоны")
                    .setHyperId(300L)
                    .setParentId(2L)
                    .setId(3L)
                    .build()
    );

    private static CategoryWalker categoryWalker;

    @BeforeClass
    public static void init() {
        DbCategoryDao categoryDao = mock(DbCategoryDao.class);
        doReturn(EXPECTED).when(categoryDao).getAll();

        categoryWalker = new CategoryWalker(categoryDao);
    }

    @Test
    public void testGetParentHyperIdIsPresent() {
        Optional<Long> actual = categoryWalker.getParentHyperIdForHyperId(300L);
        assertTrue(actual.isPresent());
        assertEquals(new Long(200L), actual.get());

        actual = categoryWalker.getParentHyperIdForHyperId(200L);
        assertTrue(actual.isPresent());
        assertEquals(new Long(100L), actual.get());
    }

    @Test
    public void testGetParentHyperIdNotPresent() {
        Optional<Long> actual = categoryWalker.getParentHyperIdForHyperId(500L);
        assertFalse(actual.isPresent());
    }

    @Test
    public void testGetParentHyperIdForRoot() {
        Optional<Long> actual = categoryWalker.getParentHyperIdForHyperId(100L);
        assertFalse(actual.isPresent());
    }
}
