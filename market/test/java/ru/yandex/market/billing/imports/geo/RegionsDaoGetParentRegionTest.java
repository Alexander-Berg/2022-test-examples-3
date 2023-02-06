package ru.yandex.market.billing.imports.geo;

import java.util.List;
import java.util.stream.Stream;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.ALL_EARTH_REGIONS;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.MOSCOW_AND_REGION;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.NORTH_WEST_FEDERAL_DISTRICT;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.SPB;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.SPB_AND_REGION;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.clearRegions;

public class RegionsDaoGetParentRegionTest extends FunctionalTest {

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
                // позитивные кейсы: искомый регион найден
                Arguments.of(
                        List.of(SPB),
                        SPB.getId(),
                        SPB.getRegionType(),
                        SPB.getId(),
                        "Указанный регион сам имеет искомый тип"),
                Arguments.of(
                        List.of(SPB, SPB_AND_REGION),
                        SPB.getId(),
                        SPB_AND_REGION.getRegionType(),
                        SPB_AND_REGION.getId(),
                        "Искомый регион является прямым родителем указанного региона"),
                Arguments.of(
                        List.of(SPB, SPB_AND_REGION, NORTH_WEST_FEDERAL_DISTRICT),
                        SPB.getId(),
                        NORTH_WEST_FEDERAL_DISTRICT.getRegionType(),
                        NORTH_WEST_FEDERAL_DISTRICT.getId(),
                        "Искомый регион является транзитивным родителем указанного региона"),
                Arguments.of(
                        ALL_EARTH_REGIONS,
                        SPB_AND_REGION.getId(),
                        NORTH_WEST_FEDERAL_DISTRICT.getRegionType(),
                        NORTH_WEST_FEDERAL_DISTRICT.getId(),
                        "Искомый регион является прямым родителем указанного региона " +
                                "(при этом присутствуют другие регионы вверх и вниз по иерархии)"),

                // негативные кейсы: искомый регион отсутствует
                Arguments.of(
                        List.of(SPB),
                        MOSCOW_AND_REGION.getId(),
                        RegionType.COUNTRY,
                        null,
                        "Указанный регион отсутствует в базе"),
                Arguments.of(
                        List.of(SPB),
                        SPB.getId(),
                        RegionType.COUNTRY,
                        null,
                        "У указанного региона отсутствуют родители"),
                Arguments.of(
                        ALL_EARTH_REGIONS,
                        SPB.getId(),
                        RegionType.REGION,
                        null,
                        "У указанного региона не найден родитель заданного типа"),

                // странные кейсы
                Arguments.of(
                        ALL_EARTH_REGIONS,
                        NORTH_WEST_FEDERAL_DISTRICT.getId(),
                        RegionType.CITY,
                        null,
                        "У указанного региона нет родителя заданного типа, но есть дочерний регион")
        );
    }

    @ParameterizedTest(name = "[{index}]: {2}")
    @MethodSource("testArguments")
    @SuppressWarnings("unused")
    public void rewriteRegionsDeletesRegionsOutOfEarth(List<Region> allRegions,
                                                       long regionIdToFindParent,
                                                       RegionType parentType,
                                                       Long expectedParentId,
                                                       String testName) {
        regionsDao.add(allRegions);
        Long parentId = regionsDao.getParent(regionIdToFindParent, parentType).orElse(null);

        assertThat(parentId).isEqualTo(expectedParentId);
    }

    // Найдено несколько родительских регионов заданного типа
    @Test
    public void strangeTestWithSeveralParentRegionsWithTheSameType() {
        Region northWestInvalidType = new Region()
                .withId(17L)
                .withName("Северо-Западный федеральный округ с типом как у Спб и ЛО")
                .withParentId(225L)
                .withRegionType(RegionType.SUBJECT_FEDERATION);

        regionsDao.add(List.of(SPB, SPB_AND_REGION, northWestInvalidType));
        Long parentId = regionsDao.getParent(SPB.getId(), RegionType.SUBJECT_FEDERATION).orElse(null);

        if (!(SPB_AND_REGION.getId().equals(parentId) || northWestInvalidType.getId().equals(parentId))) {
            Assertions.fail();
        }
    }
}
