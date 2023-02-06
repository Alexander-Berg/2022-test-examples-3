package ru.yandex.direct.regions.utils;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.io.IOUtils;

import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeLoader;
import ru.yandex.direct.regions.GeoTreeType;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TestGeoTrees {
    private TestGeoTrees() {
    }

    private static GeoTree load(String jsonFile, GeoTreeType geoTreeType) {
        try {
            String json = IOUtils.toString(TestGeoTrees.class.getResourceAsStream(jsonFile), UTF_8);
            return GeoTreeLoader.build(json, geoTreeType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static GeoTree loadGlobalTree() {
        return load("/test_regions.json", GeoTreeType.GLOBAL);
    }

    public static GeoTree loadApiTree() {
        return load("/test_regions_api.json", GeoTreeType.API);
    }

    public static GeoTree loadRussianTree() {
        return load("/test_regions_russian.json", GeoTreeType.RUSSIAN);
    }
}
