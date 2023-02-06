package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.ShopOutletProvider.MINIFIED_PROPERTIES;

public class OutletsMarketReportXmlParserTest {

    @Test
    public void shouldParseOutletsXml() throws IOException {
        OutletsMarketReportXmlParser parser = new OutletsMarketReportXmlParser(Clock.systemDefaultZone());
        parser.parse(OutletsMarketReportXmlParserTest.class.getResourceAsStream("outlets.xml"));

        List<ShopOutlet> outlets = parser.getOutlets();

        assertNotNull(outlets);
        assertThat(outlets, hasSize(1));

        ShopOutlet shopOutlet = Iterables.getOnlyElement(outlets);

        assertEquals(997393L, shopOutlet.getId().longValue());
        assertTrue(shopOutlet.isCashAllowed());
        assertTrue(shopOutlet.isPrepayAllowed());
        assertEquals(9, (int) shopOutlet.getStoragePeriod());
        assertEquals(OutletPurpose.PICKUP, shopOutlet.getPurpose());
        assertThat(shopOutlet.getScheduleString(), is(
                "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>1</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>1</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                        "                <Break>\n" +
                        "                    <HoursFrom>18:00</HoursFrom>\n" +
                        "                    <HoursTill>18:30</HoursTill>\n" +
                        "                </Break>\n" +
                        "                <Break>\n" +
                        "                    <HoursFrom>19:00</HoursFrom>\n" +
                        "                    <HoursTill>20:00</HoursTill>\n" +
                        "                </Break>\n" +
                        "            </WorkingTime>" +
                        "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>2</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>2</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                        "            </WorkingTime>" +
                        "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>3</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>3</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                        "            </WorkingTime>" +
                        "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>4</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>4</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                        "            </WorkingTime>" +
                        "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>5</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>5</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                        "            </WorkingTime>" +
                        "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>6</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>6</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>18:00</WorkingHoursTill>\n" +
                        "            </WorkingTime>" +
                        "<WorkingTime>\n" +
                        "                <WorkingDaysFrom>7</WorkingDaysFrom>\n" +
                        "                <WorkingDaysTill>7</WorkingDaysTill>\n" +
                        "                <WorkingHoursFrom>10:00</WorkingHoursFrom>\n" +
                        "                <WorkingHoursTill>18:00</WorkingHoursTill>\n" +
                        "            </WorkingTime>"));
    }

    @Test
    public void shouldParseMinifiedOutlets() throws IOException {
        OutletsMarketReportXmlParser parser = new OutletsMarketReportXmlParser(Clock.systemDefaultZone());
        parser.parse(OutletsMarketReportXmlParserTest.class.getResourceAsStream("minified-outlets.xml"));

        List<ShopOutlet> outlets = parser.getOutlets();

        assertNotNull(outlets);
        assertThat(outlets, hasSize(1));

        ShopOutlet outlet = Iterables.getOnlyElement(outlets);
        assertEquals(997393L, outlet.getId().longValue());
        assertEquals(213, outlet.getRegionId());
        assertEquals(1, outlet.getRank().intValue());
        assertEquals(1, outlet.getMinDeliveryDays().intValue());
        assertEquals(2, outlet.getMaxDeliveryDays().intValue());
        assertEquals(165, outlet.getCost().intValue());
        assertEquals(DateUtil.addDay(DateUtil.getToday(), 3), outlet.getShipmentDate());
        assertTrue(outlet.isCashAllowed());
        assertTrue(outlet.isCardAllowed());
        assertTrue(outlet.isPrepayAllowed());

        assertHasNoMinifiedValues(outlet);
    }

    private void assertHasNoMinifiedValues(ShopOutlet outlet) {
        MINIFIED_PROPERTIES.forEach(p -> {
            Object actualValue = p.apply(outlet);
            assertNull(actualValue);
        });
    }
}
