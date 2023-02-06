package ru.yandex.direct.regions;

import java.io.IOException;

import org.junit.Test;

import ru.yandex.direct.regions.utils.TestGeoTrees;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

public class GeoTreeLoaderTest {
    @Test
    public void build_ParseJson_GeoTreeServiceInit() throws IOException {
        GeoTree geoTree = TestGeoTrees.loadGlobalTree();

        assertEquals(geoTree.getRegion(GLOBAL_REGION_ID).getId(),
                geoTree.getRegion(RUSSIA_REGION_ID).getParent().getId());
    }
}
