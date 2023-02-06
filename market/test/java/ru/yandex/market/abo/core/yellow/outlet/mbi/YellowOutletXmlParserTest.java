package ru.yandex.market.abo.core.yellow.outlet.mbi;

import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.outlet.model.SimpleOutletInfo;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author komarovns
 * @date 04.02.2020
 */
class YellowOutletXmlParserTest {
    private static final double EPS = 1e-9;
    private static final String XML = "/outlet/mbi-fmcg-outlets.xml";

    @Test
    void testParse() throws Exception {
        var parserFactory = SAXParserFactory.newInstance();
        var holder = new SimpleOutletInfo[1];
        try (var is = getClass().getResourceAsStream(XML)) {
            parserFactory.newSAXParser().parse(is, new YellowOutletXmlParser(outlet -> holder[0] = outlet));
        }
        var outlet = holder[0];
        assertNotNull(outlet);
        assertEquals(111, outlet.getId());
        assertEquals(774, outlet.getShopId());
        assertEquals(63, outlet.getRegionId());
        assertEquals(104.365542, outlet.getCoordinates().getLongitude(), EPS);
        assertEquals(52.330657, outlet.getCoordinates().getLatitude(), EPS);
        assertEquals("Locality Name", outlet.getAddress().getCity());
        assertEquals("Address Add", outlet.getAddress().getAddrAdditional());
        assertEquals("1/2", outlet.getAddress().getNumber());
        assertEquals("Thoroughfare Name", outlet.getAddress().getStreet());
        assertEquals("Building", outlet.getAddress().getBuilding());
        assertEquals("Estate", outlet.getAddress().getEstate());
        assertEquals("Block", outlet.getAddress().getBlock());
        assertEquals("Post Code", outlet.getAddress().getPostCode());
        assertEquals(1, outlet.getScheduleLines().size());
        var scheduleLine = outlet.getScheduleLines().get(0);
        assertEquals(ScheduleLine.DayOfWeek.MONDAY, scheduleLine.getStartDay());
        assertEquals(6, scheduleLine.getDays());
        assertEquals(420, scheduleLine.getStartMinute());
        assertEquals(1019, scheduleLine.getMinutes());
    }
}
