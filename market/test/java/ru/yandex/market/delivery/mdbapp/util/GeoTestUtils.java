package ru.yandex.market.delivery.mdbapp.util;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.integration.service.CapacityServiceTest;
import ru.yandex.market.delivery.mdbapp.util.region.RegionTreeXmlBuilder;

public final class GeoTestUtils {

    private GeoTestUtils() {
    }

    /**
     * Region tree used (from top)
     * 0 - 10 000 - 10 001 - 225 - 3 - 1
     * 1 - 98 580
     * 1 - 213
     * 213 - 216
     * 213 - 20 279
     * 20 279 - 117 067
     * 20 279 - 117 066
     * 20 279 - 117 065
     * 117 065 - 20 481
     * 117 065 - 20 482
     */
    public static GeoInfo prepareGeoInfo() {
        return new GeoInfo(prepareRegionService());
    }

    public static RegionService prepareRegionService() {
        RegionTreeXmlBuilder regionTreeXmlBuilder = builderRegionTreeXmlBuilder();

        RegionService service = new RegionService();
        service.setRegionTreeBuilder(regionTreeXmlBuilder);
        regionTreeXmlBuilder.setSkipUnRootRegions(true);
        return service;
    }

    public static RegionTree<Region> buildRegionTree() {
        return builderRegionTreeXmlBuilder().buildRegionTree();
    }

    private static RegionTreeXmlBuilder builderRegionTreeXmlBuilder() {
        RegionTreeXmlBuilder regionTreeXmlBuilder = new RegionTreeXmlBuilder();
        regionTreeXmlBuilder.setUrl(CapacityServiceTest.class.getResource("/region-tree.xml"));
        regionTreeXmlBuilder.setSkipUnRootRegions(true);
        return regionTreeXmlBuilder;
    }
}
