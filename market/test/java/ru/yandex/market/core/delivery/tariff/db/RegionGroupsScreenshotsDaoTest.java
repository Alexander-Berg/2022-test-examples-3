package ru.yandex.market.core.delivery.tariff.db;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.AboScreenshot;
import ru.yandex.market.core.delivery.tariff.db.dao.RegionGroupsScreenshotsDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link RegionGroupsScreenshotsDao}.
 */
class RegionGroupsScreenshotsDaoTest extends FunctionalTest {

    @Autowired
    private RegionGroupsScreenshotsDao regionGroupsScreenshotsDao;

    @Test
    @DisplayName("Получение скриншотов, привязанных к множеству региональных групп доставки")
    @DbUnitDataSet(before = "csv/RegionGroupsScreenshotsDao.select.before.csv")
    void testGetScreenshotsLinkedToRegionGroups() {
        final var regionGroups = List.of(1001L, 1002L, 9999L);
        final var actual = regionGroupsScreenshotsDao.getScreenshotsLinkedToRegionGroups(regionGroups);

        assertThat(actual.get(1001L), containsInAnyOrder(
                new AboScreenshot(101L, 17L, "hash1")
        ));

        assertThat(actual.get(1002L), containsInAnyOrder(
                new AboScreenshot(102L, 19L, "hash2"),
                new AboScreenshot(103L, 23L, "hash3")
        ));

        assertNull(actual.get(9999L));
    }

    @Test
    @DisplayName("Получение скриншотов по пустому множеству региональных групп")
    @DbUnitDataSet(before = "csv/RegionGroupsScreenshotsDao.select.before.csv")
    void testGetScreenshotsWithEmptyRegionGroupList() {
        final var actual = regionGroupsScreenshotsDao.getScreenshotsLinkedToRegionGroups(Collections.emptyList());
        assertTrue(actual.isEmpty());
    }

}
