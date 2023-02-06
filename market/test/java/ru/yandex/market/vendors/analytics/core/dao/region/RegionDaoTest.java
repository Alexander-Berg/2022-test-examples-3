package ru.yandex.market.vendors.analytics.core.dao.region;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.model.region.RegionInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests for {@link RegionDao}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "PostgresRegionDAOTest.before.csv")
public class RegionDaoTest extends FunctionalTest {

    @Autowired
    private RegionDao regionDao;

    @Test
    @DisplayName("Проверка загрузки регионов по id")
    void testLoadRegionsByIds() {
        List<RegionInfo> expected = Arrays.asList(
                new RegionInfo(1L, "Россия", "Russia", 1, 0L),
                new RegionInfo(2L, "Москва", "Moscow", 2, 1L)
        );
        List<RegionInfo> loaded = regionDao.loadRegions(ImmutableList.of(1L, 2L, 3L));
        assertEquals(expected, loaded);
    }

    @Test
    @DisplayName("Проверка загрузки регионов по id: пустой список айдишников")
    void testLoadRegionsByIdsEmptyParam() {
        List<RegionInfo> expected = Collections.emptyList();
        List<RegionInfo> loaded = regionDao.loadRegions(Collections.emptyList());
        assertEquals(expected, loaded);
    }
}
