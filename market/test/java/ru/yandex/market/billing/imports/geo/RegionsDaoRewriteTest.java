package ru.yandex.market.billing.imports.geo;

import java.util.List;
import java.util.stream.Stream;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;

import static java.util.Collections.emptyList;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.ALL_EARTH_REGIONS;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.ALL_MARS_REGIONS;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.ALL_REGIONS;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.CENTRAL_FEDERAL_DISTRICT;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.EARTH;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.EURASIA;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.MOSCOW_AND_REGION;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.NORTH_WEST_FEDERAL_DISTRICT;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.RUSSIA;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.checkRegions;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.clearRegions;

public class RegionsDaoRewriteTest extends FunctionalTest {

    @Autowired
    private RegionsDao regionsDao;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    public void beforeEach() {
        clearRegions(dslContext);
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
                Arguments.of(List.of(EARTH), List.of(EARTH), "Только Земля"),
                Arguments.of(List.of(EARTH, EURASIA), List.of(EARTH, EURASIA), "Земля и Евразия"),
                Arguments.of(ALL_EARTH_REGIONS, ALL_EARTH_REGIONS, "Разветвлённое дерево регионов Земли"),
                Arguments.of(ALL_REGIONS, ALL_EARTH_REGIONS, "Разветвлённое дерево регионов Земли + регионы Марса"),
                Arguments.of(ALL_MARS_REGIONS, emptyList(), "Регионы Марса"),
                Arguments.of(List.of(EARTH, EURASIA, MOSCOW_AND_REGION), List.of(EARTH, EURASIA),
                        "Присутствует регион с отсутствующим в дереве родителем")
        );
    }

    @ParameterizedTest(name = "[{index}]: {2}")
    @MethodSource("testArguments")
    @SuppressWarnings("unused")
    public void rewriteRegionsDeletesRegionsOutOfEarth(List<Region> inputRegions,
                                                       List<Region> expectedRegions,
                                                       String testName) {
        regionsDao.rewriteAllRegions(inputRegions);
        checkRegions(regionsDao.get(), expectedRegions);
    }

    @Test
    public void rewriteRegionsDeletesPreviousRegions() {
        regionsDao.add(List.of(CENTRAL_FEDERAL_DISTRICT));
        List<Region> newRegions = List.of(EARTH, EURASIA, RUSSIA, NORTH_WEST_FEDERAL_DISTRICT);
        regionsDao.rewriteAllRegions(newRegions);
        checkRegions(regionsDao.get(), newRegions);
    }
}
