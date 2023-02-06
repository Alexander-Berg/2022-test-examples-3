package ru.yandex.market.crm.external.report;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.domain.report.Outlet;

/**
 * @author vtarasoff
 * @since 27.02.2021
 */
public class OutletParserTest {
    @Test
    public void testParser_oneOutlet() {
        ListParser<Outlet> listParser = new ListParser(new OutletParser());
        List<Outlet> outlets = listParser.parse(getClass().getResourceAsStream("outlet_response.json"));

        Assertions.assertNotNull(outlets);
        Assertions.assertEquals(1, outlets.size());

        Outlet outlet = outlets.get(0);

        Assertions.assertSame(123L, outlet.getId());
        Assertions.assertEquals("ПВЗ Яндекс.Маркета", outlet.getName());
        Assertions.assertTrue(outlet.isMarketPartner());
        Assertions.assertTrue(outlet.isMarketPostTerm());
        Assertions.assertEquals("pickup", outlet.getPurpose());

        Outlet.Address address = outlet.getAddress();
        Assertions.assertNotNull(address);

        Assertions.assertEquals("Москва, Какая-то улица, 1", address.getFullAddress());
        Assertions.assertEquals("Едьте прямо, прямо, прямо... и доедете", address.getNote());

        List<Outlet.WorkingTime> workingTime = outlet.getWorkingTime();
        Assertions.assertNotNull(workingTime);
        Assertions.assertSame(2, workingTime.size());

        Assertions.assertEquals(Integer.valueOf(1), workingTime.get(0).getDaysFrom());
        Assertions.assertEquals(Integer.valueOf(1), workingTime.get(0).getDaysTo());
        Assertions.assertEquals("08:00", workingTime.get(0).getHoursFrom());
        Assertions.assertEquals("20:00", workingTime.get(0).getHoursTo());

        Assertions.assertEquals(Integer.valueOf(2), workingTime.get(1).getDaysFrom());
        Assertions.assertEquals(Integer.valueOf(2), workingTime.get(1).getDaysTo());
        Assertions.assertEquals("10:00", workingTime.get(1).getHoursFrom());
        Assertions.assertEquals("22:00", workingTime.get(1).getHoursTo());
    }

    @Test
    public void testParser_twoOutlets() {
        ListParser<Outlet> listParser = new ListParser(new OutletParser());
        List<Outlet> outlets = listParser.parse(getClass().getResourceAsStream("outlets_response.json"));

        Assertions.assertNotNull(outlets);
        Assertions.assertEquals(2, outlets.size());

        Outlet outlet1 = outlets.get(0);

        Assertions.assertEquals(123L, outlet1.getId());
        Assertions.assertTrue(outlet1.isMarketPartner());
        Assertions.assertFalse(outlet1.isMarketPostTerm());

        Outlet.GpsCoord gps1 = outlet1.getGpsCoord();
        Assertions.assertNotNull(gps1);

        Assertions.assertEquals(56.85436956, gps1.getLatitude(), 0.000000001);
        Assertions.assertEquals(60.6349881, gps1.getLongitude(), 0.000000001);

        Outlet outlet2 = outlets.get(1);
        Assertions.assertEquals(345L, outlet2.getId());
        Assertions.assertFalse(outlet2.isMarketPartner());
        Assertions.assertTrue(outlet2.isMarketPostTerm());

        Outlet.GpsCoord gps2 = outlet2.getGpsCoord();
        Assertions.assertNotNull(gps2);

        Assertions.assertEquals(56.8, gps2.getLatitude(), 0.000000001);
        Assertions.assertEquals(60.1, gps2.getLongitude(), 0.000000001);
    }
}
