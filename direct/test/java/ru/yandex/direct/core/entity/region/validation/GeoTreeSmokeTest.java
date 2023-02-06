package ru.yandex.direct.core.entity.region.validation;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeLoader;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.regions.Region;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class GeoTreeSmokeTest {
    private static GeoTree geoTree;

    @BeforeClass
    public static void beforeClass() {
        geoTree = GeoTreeLoader
                .build(LiveResourceFactory.get("classpath:///externalData/regions.json").getContent(), GeoTreeType.GLOBAL);
    }

    @Test
    public void ensureContainsMoscow() {
        assertThat(geoTree.hasRegion(Region.MOSCOW_REGION_ID), is(true));
    }

    @Test
    public void smokeTestRegions() {
        assertEquals(geoTree.getRegion(0L).getId(), geoTree.getRegions().get(225L).getParent().getId());
    }

    @Test
    public void smokeTestMetros() {
        assertEquals("у метро \"Невский Проспект\" родитель должен быть Петербург",
                geoTree.getMetro(20347L).getParent().getId(),
                2);
    }
}
