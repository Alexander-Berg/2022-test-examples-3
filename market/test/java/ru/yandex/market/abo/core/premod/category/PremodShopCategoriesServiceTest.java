package ru.yandex.market.abo.core.premod.category;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author artemmz
 *         created on 14.03.17.
 */
class PremodShopCategoriesServiceTest extends EmptyTest {
    @Autowired
    private PremodShopCategoriesService premodShopCategoriesService;

    @Test
    void testDAO() {
        for (int i = 0; i < 2; i++) {
            long shopId = 774;
            List<Long> categories = Arrays.asList(RND.nextLong(), RND.nextLong(), RND.nextLong());
            premodShopCategoriesService.saveShopCategories(shopId, categories);
            Map<Long, List<Long>> dbCategoriesByShop = premodShopCategoriesService.getShopsPremodCategories(List.of(shopId));
            assertEquals(categories, dbCategoriesByShop.get(shopId));
        }
        premodShopCategoriesService.getEnabledShopsWithSavedCategories();
    }

    @Test
    void getShopPremodCategoriesTest() {
        premodShopCategoriesService.saveShopCategories(1L, Arrays.asList(1L,2L,3L));
        Map<Long, List<Long>> dbCategoriesByShop = premodShopCategoriesService.getShopsPremodCategories(List.of(1L, 2L));
        assertEquals(Arrays.asList(1L, 2L, 3L), dbCategoriesByShop.get(1L));
        assertFalse(dbCategoriesByShop.containsKey(2L));
    }
}
