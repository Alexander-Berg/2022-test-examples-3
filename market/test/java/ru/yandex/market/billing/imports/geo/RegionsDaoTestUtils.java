package ru.yandex.market.billing.imports.geo;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.jooq.DSLContext;

import ru.yandex.common.util.region.RegionType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.market.billing.dbschema.shops_web.Tables.REGIONS;

public class RegionsDaoTestUtils {

    // От Земли до России
    public static final Region EARTH = new Region()
            .withId(10000L).withName("Земля").withParentId(null).withRegionType(RegionType.OTHERS_UNIVERSAL);
    public static final Region EURASIA = new Region()
            .withId(10001L).withName("Евразия").withParentId(10000L).withRegionType(RegionType.CONTINENT);
    public static final Region RUSSIA = new Region()
            .withId(225L).withName("Россия").withParentId(10001L).withRegionType(RegionType.COUNTRY);

    // От России до Ленобласти
    public static final Region NORTH_WEST_FEDERAL_DISTRICT = new Region()
            .withId(17L)
            .withName("Северо-Западный федеральный округ")
            .withParentId(225L)
            .withRegionType(RegionType.COUNTRY_DISTRICT);
    public static final Region SPB_AND_REGION = new Region()
            .withId(10174L)
            .withName("Санкт-Петербург и Ленинградская область")
            .withParentId(17L)
            .withRegionType(RegionType.SUBJECT_FEDERATION);

    // Города и районы в Ленобласти
    public static final Region SPB = new Region()
            .withId(2L)
            .withName("Санкт-Петербург")
            .withParentId(10174L)
            .withRegionType(RegionType.CITY);
    public static final Region PRIOZERSKIY_DISTRICT = new Region()
            .withId(98631L)
            .withName("Приозерский район")
            .withParentId(10174L)
            .withRegionType(RegionType.SUBJECT_FEDERATION_DISTRICT);

    // От России до ЦФО
    public static final Region CENTRAL_FEDERAL_DISTRICT = new Region()
            .withId(3L)
            .withName("Центральный федеральный округ")
            .withParentId(225L)
            .withRegionType(RegionType.COUNTRY_DISTRICT);
    public static final Region MOSCOW_AND_REGION = new Region()
            .withId(1L)
            .withName("Москва и Московская область")
            .withParentId(3L)
            .withRegionType(RegionType.SUBJECT_FEDERATION);

    public static final List<Region> ALL_EARTH_REGIONS = ImmutableList.of(
            EARTH, EURASIA, RUSSIA, NORTH_WEST_FEDERAL_DISTRICT, SPB_AND_REGION, SPB, PRIOZERSKIY_DISTRICT,
            CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_REGION
    );

    public static final Region MARS = new Region()
            .withId(99999999L).withName("Марс").withParentId(null).withRegionType(RegionType.OTHERS_UNIVERSAL);

    public static final Region PHOBOS = new Region()
            .withId(999991L).withName("Фобос").withParentId(99999999L).withRegionType(RegionType.SUBJECT_FEDERATION);

    public static final Region DEIMOS = new Region()
            .withId(999992L).withName("Деймос").withParentId(99999999L).withRegionType(RegionType.SUBJECT_FEDERATION);

    public static final List<Region> ALL_MARS_REGIONS = ImmutableList.of(
            MARS, PHOBOS, DEIMOS
    );

    public static final List<Region> ALL_REGIONS = ImmutableList.<Region>builder()
            .addAll(ALL_EARTH_REGIONS)
            .addAll(ALL_MARS_REGIONS)
            .build();

    private RegionsDaoTestUtils() {
    }

    public static void clearRegions(DSLContext dslContext) {
        dslContext.deleteFrom(REGIONS).execute();
    }

    public static void checkRegions(List<Region> actualRegions, List<Region> expectedRegions) {
        Map<Long, Region> actualRegionsMap = listToMap(actualRegions, Region::getId);
        Map<Long, Region> expectedRegionsMap = listToMap(expectedRegions, Region::getId);

        assertThat(actualRegionsMap.keySet()).as("actual region ids match expected region ids")
                .isEqualTo(expectedRegionsMap.keySet());

        expectedRegionsMap.forEach((id, expectedRegion) -> checkRegion(actualRegionsMap.get(id), expectedRegion));
    }

    public static void checkRegion(Region actual, Region expected) {
        assertThat(actual).as("region exists").isNotNull();
        assertThat(actual.getId()).as("id match").isEqualTo(expected.getId());
        assertThat(actual.getName()).as("name match").isEqualTo(expected.getName());
        assertThat(actual.getParentId()).as("parentId match").isEqualTo(expected.getParentId());
        assertThat(actual.getRegionType()).as("regionType match").isEqualTo(expected.getRegionType());
    }
}
