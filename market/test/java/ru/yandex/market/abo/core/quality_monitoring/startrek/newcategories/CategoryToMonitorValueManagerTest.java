package ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.premod.category.PremodShopCategoriesService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.03.2020
 */
class CategoryToMonitorValueManagerTest {

    private static final long CATEGORY = 1L;
    private static final String CATEGORY_NAME = "foo";
    private static final long THRESHOLD = 10;
    private static final long SHOP_ID = 123L;

    private CategoryToMonitorValueManager categoryToMonitorValueManager;

    @Mock
    private ShopCategoriesToWatchRepository monitorCategoriesToRepo;
    @Mock
    private PremodShopCategoriesService shopCategoriesService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(monitorCategoriesToRepo.findByMonitoringScope(MonitoringScope.CATEGORY_CHANGE))
                .thenReturn(List.of(buildCategoryToMonitor(CATEGORY), buildCategoryToMonitor(CATEGORY + 1)));

        when(shopCategoriesService.getShopsPremodCategories(List.of(SHOP_ID))).thenReturn(Map.of(SHOP_ID, List.of(CATEGORY)));

        categoryToMonitorValueManager = new CategoryToMonitorValueManager(monitorCategoriesToRepo, shopCategoriesService);
    }

    @Test
    void searchNewCategoriesWithoutNewCategories() {
        var newCategories = categoryToMonitorValueManager
                .searchNewCategories(List.of(SHOP_ID), List.of(buildShopCategoryOffersCount(CATEGORY, THRESHOLD)));
        assertTrue(newCategories.get(SHOP_ID).isEmpty());
    }

    @Test
    void searchNewCategoriesWithCategoriesUnderThreshold() {
        var newCategories = categoryToMonitorValueManager
                .searchNewCategories(List.of(SHOP_ID), List.of(buildShopCategoryOffersCount(CATEGORY + 1, THRESHOLD + 1)));
        assertEquals(1L, newCategories.size());
        assertEquals(
                Set.of(new CategoryToMonitorValue(buildCategoryToMonitor(CATEGORY + 1), THRESHOLD + 1)),
                new HashSet<>(newCategories.get(SHOP_ID))
        );
    }

    private static ShopCategoryOffersCount buildShopCategoryOffersCount(long category, long offersCount) {
        return new ShopCategoryOffersCount(SHOP_ID, category, offersCount);
    }

    private static CategoryToMonitor buildCategoryToMonitor(long category) {
        return new CategoryToMonitor(category, CATEGORY_NAME, THRESHOLD, MonitoringScope.CATEGORY_CHANGE);
    }
}
