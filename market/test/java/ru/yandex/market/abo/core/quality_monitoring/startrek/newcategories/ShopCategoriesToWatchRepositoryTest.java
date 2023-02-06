package ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 *         created on 17.03.17.
 */
public class ShopCategoriesToWatchRepositoryTest extends EmptyTest {
    private static final long CATEGORY_ID = RND.nextLong();
    private static final CategoryToMonitor category =
            new CategoryToMonitor(CATEGORY_ID, "fooCat", RND.nextLong(), MonitoringScope.CATEGORY_CHANGE);

    @Autowired
    private ShopCategoriesToWatchRepository shopCategoriesToWatchRepository;

    @BeforeEach
    public void setUp() throws Exception {
        shopCategoriesToWatchRepository.save(category);
    }

    @Test
    public void testDAO() throws Exception {
        CategoryToMonitor dbCategory = shopCategoriesToWatchRepository.findByIdOrNull(category.getId());
        assertEqualCategories(category, dbCategory);
    }

    @Test
    public void findByCategoryAndScope() throws Exception {
        CategoryToMonitor saved = shopCategoriesToWatchRepository.findByCategoryIdAndMonitoringScope(
                category.getCategoryId(), category.getMonitoringScope());
        assertEqualCategories(category, saved);
    }

    @Test
    public void findByScope() throws Exception {
        List<CategoryToMonitor> scopeCategories = shopCategoriesToWatchRepository.findByMonitoringScope(
                category.getMonitoringScope());
        assertTrue(scopeCategories.contains(category));
    }

    private void assertEqualCategories(CategoryToMonitor cat1, CategoryToMonitor cat2) {
        assertEquals(cat1.getCategoryId(), cat2.getCategoryId());
        assertEquals(cat1.getThreshold(), cat2.getThreshold());
        assertEquals(cat1.getCategoryName(), cat2.getCategoryName());
        assertEquals(cat1.getMonitoringScope(), cat2.getMonitoringScope());
    }
}
