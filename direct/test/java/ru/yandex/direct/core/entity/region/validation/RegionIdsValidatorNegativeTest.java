package ru.yandex.direct.core.entity.region.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeLoader;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoEmptyRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoIncorrectRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoMinusRegionMatchesPlusRegion;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoMinusRegionsWithoutPlusRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoNoPlusRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoNonUniqueRegions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class RegionIdsValidatorNegativeTest {

    private static RegionIdsValidator validator;
    private static GeoTree geoTree;

    @SuppressWarnings("DefaultAnnotationParam")
    @Parameterized.Parameter(0)
    public List<Long> regionIds;

    @Parameterized.Parameter(1)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "Регион: {0}")
    public static Collection<Object[]> parameters() {
        GeoTree geo = GeoTreeLoader.build(
                LiveResourceFactory.get("classpath:///externalData/regions.json").getContent(), GeoTreeType.GLOBAL);
        return Arrays.asList(new Object[][]{
                // пустой регион
                {
                        new ArrayList<>(),
                        geoEmptyRegions()
                },
                {
                        singletonList(null),
                        geoEmptyRegions()
                },
                {
                        singletonList(-10895L),
                        geoNoPlusRegions()
                },
                {
                        asList(-10895L, -193L),
                        geoNoPlusRegions()
                },
                // несуществующий регион
                {
                        singletonList(471L),
                        geoIncorrectRegions("471")
                },
                {
                        asList(471L, 300L),
                        geoIncorrectRegions("471,300")
                },
                {
                        asList(225L, 471L),
                        geoIncorrectRegions("471")
                },
                {
                        asList(225L, -471L),
                        geoIncorrectRegions("-471")
                },
                {
                        asList(225L, -471L, -300L),
                        geoIncorrectRegions("-471,-300")
                },
                {
                        asList(225L, -471L, 114L, -300L),
                        geoIncorrectRegions("-471,-300")
                },
                // совпадение плюс- и минус-регионов
                {
                        asList(225L, -10895L, -193L, 10895L),
                        geoMinusRegionMatchesPlusRegion("-10895", "10895")
                },
                // повторяющийся регион
                {
                        asList(225L, 225L),
                        geoNonUniqueRegions(singletonList(geo.getRegion(225L)))
                },
                {
                        asList(10895L, 225L, 114L, 225L),
                        geoNonUniqueRegions(singletonList(geo.getRegion(225L)))
                },
                {
                        asList(225L, -10895L, -193L, 114L, -193L),
                        geoNonUniqueRegions(singletonList(geo.getRegion(193L)))
                },
                // минус-регион, не содержащийся в плюс-регионе
                {
                        asList(-10895L, 193L),
                        geoMinusRegionsWithoutPlusRegions(singletonList(geo.getRegion(10895L)))
                },
                {
                        asList(225L, -983L),
                        geoMinusRegionsWithoutPlusRegions(singletonList(geo.getRegion(983L)))
                },
                {
                        asList(225L, -983L, -114L),
                        geoMinusRegionsWithoutPlusRegions(asList(geo.getRegion(983L), geo.getRegion(114L)))
                },
                {
                        asList(1L, 225L, -983L, -114L),
                        geoMinusRegionsWithoutPlusRegions(asList(geo.getRegion(983L), geo.getRegion(114L)))
                },
                {
                        asList(225L, -10895L, -983L),
                        geoMinusRegionsWithoutPlusRegions(singletonList(geo.getRegion(983L)))
                },
                {
                        asList(225L, -983L, 114L, -10895L),
                        geoMinusRegionsWithoutPlusRegions(singletonList(geo.getRegion(983L)))
                },
        });
    }

    @BeforeClass
    public static void setup() {
        String json = LiveResourceFactory.get("classpath:///externalData/regions.json").getContent();
        geoTree = GeoTreeLoader.build(json, GeoTreeType.GLOBAL);
        validator = new RegionIdsValidator();
    }

    @Test
    public void testValidCase() {
        ValidationResult<List<Long>, Defect> result = validator.apply(regionIds, geoTree);
        assertThat("ошибка в результате валидации не соответствует ожидаемой",
                result.flattenErrors(), contains(validationError(path(), expectedDefect)));
    }
}
