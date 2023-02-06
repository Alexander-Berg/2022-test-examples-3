package ru.yandex.market.pers.notify.service;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author semin-serg
 */
public class GeoExportServiceTest {

    private static final Logger log = Logger.getLogger(GeoExportServiceTest.class);

    @Test
    public void testGoodResponse() {
        final String response =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<regions>\n" +
            "    <region ru_preposition=\"в\" ru_prepositional=\"Москве\" />\n" +
            "</regions>\n";
        GeoExportService.Region region = GeoExportService.parseResponse(response);
        assertNotNull(region);
        assertEquals("в", region.getRuPreposition());
        assertEquals("Москве", region.getRuPrepositional());
    }

    @Test
    public void testResponseWithEmptyPrepositional() {
        final String response =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<regions>\n" +
            "    <region ru_preposition=\"в\" ru_prepositional=\"\" />\n" +
            "</regions>\n";
        GeoExportService.Region region = GeoExportService.parseResponse(response);
        assertNull(region);
    }

    @Test
    public void testResponseWithNullPrepositional() {
        final String response =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<regions>\n" +
                "    <region ru_preposition=\"в\"/>\n" +
                "</regions>\n";
        GeoExportService.Region region = GeoExportService.parseResponse(response);
        assertNull(region);
    }

    @Test
    public void testResponseWithEmptyPreposition() {
        final String response =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<regions>\n" +
            "    <region ru_preposition=\"\" ru_prepositional=\"Москве\" />\n" +
            "</regions>\n";
        GeoExportService.Region region = GeoExportService.parseResponse(response);
        assertNull(region);
    }

    @Test
    public void testResponseWithNullPreposition() {
        final String response =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<regions>\n" +
                "    <region ru_prepositional=\"Москве\" />\n" +
                "</regions>\n";
        GeoExportService.Region region = GeoExportService.parseResponse(response);
        assertNull(region);
    }

    @Test
    public void testResponseWithoutRegions() {
        final String response =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<regions>\n" +
            "</regions>\n";
        GeoExportService.Region region = GeoExportService.parseResponse(response);
        assertNull(region);
    }

    @Disabled
    @Test
    public void testRealService() {
        GeoExportService geoExportService = new GeoExportService("http://geoexport-test.n.yandex-team.ru/");
        final int regionId = 213;
        GeoExportService.Region region = geoExportService.getPrepositionalRegionName(regionId);
        assertNotNull(region);
        log.info("region: " + region);
    }

}
