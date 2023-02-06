package ru.yandex.market.abo.core.category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author kukabara
 */
class CategoryManagerTest extends EmptyTest {
    @Autowired
    private CategoryManager categoryManager;

    @Test
    void loadCategories() {
        List<NavigationalCategory> categories = categoryManager.loadCategories();
        assertFalse(categories.isEmpty());
    }

    @Test
    void testUpdateCategories() {
        int id = 1;
        NavigationalCategory category1 = new NavigationalCategory(id, "Test1", 0);
        NavigationalCategory category2 = new NavigationalCategory(2, "Test2", id);
        categoryManager.updateCategories(Arrays.asList(category1, category2));

        NavigationalCategory loadedCategory = categoryManager.getById(id);
        assertEquals(category1, loadedCategory);

        category1.setParentId(3);
        categoryManager.updateCategories(Collections.singletonList(category1));
        loadedCategory = categoryManager.getById(id);
        assertEquals(category1, loadedCategory);
    }

    @Test
    void loadLeafChildren() {
        List<Integer> childrenOfFootball = categoryManager.loadLeafChildren(14369654);
        assertEquals(Arrays.asList(14304975, 396901), childrenOfFootball);
        assertEquals(Collections.emptyList(), categoryManager.loadLeafChildren(7815017));
    }

    @Test
    void validateCategories() {
        assertThrows(IllegalArgumentException.class,
                () -> categoryManager.validateMarketCategories(Set.of(-1, -2, -3)));
    }
}
