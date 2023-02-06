package ru.yandex.direct.core.entity.region.validation;

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
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class RegionIdsValidatorPositiveTest {

    private static RegionIdsValidator validator;
    private static GeoTree geoTree;

    @Parameterized.Parameter
    public List<Long> regionIds;

    @Parameterized.Parameters(name = "Регион: {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {singletonList(0L)},
                {asList(0L, -114L)},
                {asList(-114L, 0L)},
                {asList(114L, 0L)},
                {asList(0L, 114L)},
                {singletonList(225L)},
                {asList(125L, 114L)},
                {asList(225L, -10895L)},
                {asList(225L, -10895L, -193L)},
                {asList(114L, 225L, -10895L, -193L, 125L)},
                {asList(10174L, -969L, 1L, -98596L, -20728L)},
                {asList(225L, 114L, -10895L)}, // здесь минус-регион есть в первом регионе, но не во втором

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
        assertThat("результат валидации не должен содержать ошибок", result.hasAnyErrors(), is(false));
    }
}
