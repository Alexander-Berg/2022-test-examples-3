package ru.yandex.direct.core.entity.moderation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.FOREX;

@RunWith(Parameterized.class)
public class ModerationCheckUtilsIsRemoderationRequiredForBannerByGeoChangeTest {
    private static GeoTree geoTree;

    @Parameterized.Parameter
    public BannerFlags bannerFlags;
    @Parameterized.Parameter(value = 1)
    public Set<Long> oldGeoId;
    @Parameterized.Parameter(value = 2)
    public Set<Long> newGeoId;
    @Parameterized.Parameter(value = 3)
    public boolean expectedResult;

    @BeforeClass
    public static void setUpClass() {
        geoTree = TestGeoTrees.loadGlobalTree();
    }

    @Parameterized.Parameters(name = "{0} - {1} - {2} - {3}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{null, emptySet(), emptySet(), false},
                new Object[]{new BannerFlags(), emptySet(), emptySet(), false},
                new Object[]{new BannerFlags(), singleton(Region.BY_REGION_ID), singleton(Region.MOSCOW_REGION_ID),
                        false},
                new Object[]{new BannerFlags().with(FOREX, true), singleton(Region.BY_REGION_ID),
                        singleton(Region.MOSCOW_REGION_ID), true});
    }

    @Test
    public void test() {
        boolean actualResult = ModerationCheckUtils.isRemoderationRequiredForBannerByGeoChange(
                geoTree, bannerFlags, oldGeoId, newGeoId);

        assertThat(actualResult)
                .as("Флаг перемодерации для объявления не совпадает с ожидаемым")
                .isEqualTo(expectedResult);
    }
}
