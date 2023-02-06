package ru.yandex.market.replenishment.autoorder.repository;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Region;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RegionRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class RegionRepositoryTest extends FunctionalTest {
    @Autowired
    RegionRepository regionRepository;

    @Test
    @DbUnitDataSet(before = "RegionRepositoryTest.testCheckRegionTypes.before.csv")
    public void testCheckRegionTypes() {
        List<Long> regionIds = List.of(1L, 2L, 3L);
        List<Long> regionTypes = List.of(4L, 5L);
        List<Region> regions = regionRepository.checkRegionTypes(regionIds, regionTypes);
        assertEquals(1, regions.size());
        assertEquals(2, regions.get(0).getId());
        assertEquals(0, regions.get(0).getParentId());
        assertEquals("region2", regions.get(0).getName());
    }
}
