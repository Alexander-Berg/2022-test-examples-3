package ru.yandex.market.logistics.management.repository.geoBase;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.RegionEntity;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@SuppressWarnings("checkstyle:MagicNumber")
class GeoBaseRepositoryTest extends AbstractContextualTest {
    @Autowired
    private GeoBaseRepository geoBaseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @CleanDatabase
    @DatabaseSetup("/data/repository/geo-base/region-entities.xml")
    void convertTypeFromDb() {
        Map<Integer, RegionEntity> regions = transactionTemplate
                .execute((status) -> geoBaseRepository.findAll())
                .stream()
                .collect(Collectors.toMap(RegionEntity::getId, x -> x));

        softly.assertThat(regions.get(1).getType() == RegionType.HIDDEN);
        softly.assertThat(regions.get(2).getType() == RegionType.OTHERS_UNIVERSAL);
        softly.assertThat(regions.get(3).getType() == RegionType.CONTINENT);
        softly.assertThat(regions.get(4).getType() == RegionType.UNIVERSAL);
    }

    @Test
    @CleanDatabase
    void convertTypeToDb() {
        transactionTemplate.execute((status) ->
                geoBaseRepository.saveAll(Arrays.asList(
                        new RegionEntity().setId(1).setName("Test 1").setType(RegionType.HIDDEN),
                        new RegionEntity().setId(2).setName("Test 2").setType(RegionType.OTHERS_UNIVERSAL),
                        new RegionEntity().setId(3).setName("Test 3").setType(RegionType.CONTINENT),
                        new RegionEntity().setId(4).setName("Test 4").setType(RegionType.UNIVERSAL)
                ))
        );
        assertRegionType(1, RegionType.HIDDEN);
        assertRegionType(2, RegionType.OTHERS_UNIVERSAL);
        assertRegionType(3, RegionType.CONTINENT);
        assertRegionType(4, RegionType.UNIVERSAL);
    }

    private void assertRegionType(Integer id, RegionType type) {
        softly.assertThat(jdbcTemplate.queryForObject("SELECT type FROM regions WHERE id=" + id, Integer.class))
                .as("Region type " + type + " code should be " + type.getCode()).isEqualTo(type.getCode());
    }
}
