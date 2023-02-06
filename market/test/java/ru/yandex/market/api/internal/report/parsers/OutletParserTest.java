package ru.yandex.market.api.internal.report.parsers;

import org.junit.Test;
import org.mockito.InjectMocks;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.outlet.Outlet;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
public class OutletParserTest extends UnitTestBase {

    @InjectMocks
    ReportParserFactory parserFactory;

    @Test
    public void shouldParsePartialXml() throws Exception {
        Outlet outlet = new OutletParser().parse(ResourceHelpers.getResource("outlet-part.xml"));

        assertNotNull(outlet);

        assertEquals("248911", outlet.getPointId());
        assertEquals("Watch.Su", outlet.getPointName());
        assertEquals("DEPOT", outlet.getPointType());

        assertEquals("Москва", outlet.getLocalityName());
        assertEquals("проспект Маршала Жукова", outlet.getThoroughfareName());
        assertEquals(null, outlet.getBlock());
        assertEquals(null, outlet.getBuilding());
        assertEquals("4", outlet.getPremiseNumber());

        assertNotNull(outlet.getSchedule());
        assertEquals(2, outlet.getSchedule().size());

        assertEquals("1", outlet.getSchedule().get(0).getWorkingDaysFrom());
        assertEquals("5", outlet.getSchedule().get(0).getWorkingDaysTill());
        assertEquals("8:00", outlet.getSchedule().get(0).getWorkingHoursFrom());
        assertEquals("20:00", outlet.getSchedule().get(0).getWorkingHoursTill());

        assertEquals("6", outlet.getSchedule().get(1).getWorkingDaysFrom());
        assertEquals("6", outlet.getSchedule().get(1).getWorkingDaysTill());
        assertEquals("9:00", outlet.getSchedule().get(1).getWorkingHoursFrom());
        assertEquals("18:00", outlet.getSchedule().get(1).getWorkingHoursTill());


        assertNotNull(outlet.getPhone());
        assertEquals("7", outlet.getPhone().getCountry());
        assertEquals("495", outlet.getPhone().getCity());
        assertEquals("220-4477", outlet.getPhone().getNumber());


        assertEquals(null, outlet.getGeo().getDistance());
        assertEquals(null, outlet.getGeo().getDistance());
        assertEquals(null, outlet.getGeo().getDistance());


        assertEquals(null, outlet.getCmid());
        assertEquals(3284, (long) outlet.getShopId());
        assertEquals(null, outlet.getShopName());
    }
}
