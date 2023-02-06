package ru.yandex.direct.core.entity.moderation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ModerationCheckUtilsIsRemoderationRequiredByGeoChangeTest {
    private static GeoTree geoTree;

    @Parameterized.Parameter
    public Set<Long> oldGeoId;
    @Parameterized.Parameter(value = 1)
    public Set<Long> newGeoId;
    @Parameterized.Parameter(value = 2)
    public boolean expectedResult;

    @BeforeClass
    public static void setUpClass() {
        geoTree = TestGeoTrees.loadGlobalTree();
    }

    @Parameterized.Parameters(name = "{0} - {1} - {2}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{emptySet(), emptySet(), false},
                new Object[]{singleton(Region.MOSCOW_REGION_ID), singleton(Region.RUSSIA_REGION_ID), false},
                new Object[]{singleton(Region.RUSSIA_REGION_ID), singleton(Region.KAZAKHSTAN_REGION_ID), true});
    }

    @Test
    public void test() {
        boolean actualResult = ModerationCheckUtils.isRemoderationRequiredByGeoChange(
                geoTree, oldGeoId, newGeoId);

        assertThat(actualResult)
                .as("Флаг перемодерации не совпадает с ожидаемым")
                .isEqualTo(expectedResult);
    }
}
