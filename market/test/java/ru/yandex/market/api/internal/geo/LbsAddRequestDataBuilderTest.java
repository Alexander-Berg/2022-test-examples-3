package ru.yandex.market.api.internal.geo;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.GeoCoordinatesV2;
import ru.yandex.market.api.internal.geo.domain.GsmSignal;
import ru.yandex.market.api.internal.geo.domain.SignalsInfo;
import ru.yandex.market.api.internal.geo.domain.WifiSignal;

import java.net.URLEncoder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class LbsAddRequestDataBuilderTest {

    private Clock clock;

    @Before
    public void setUp() throws Exception {
        clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
    }

    @Test
    public void shouldBuildData() throws Exception {
        GeoCoordinatesV2 coordinates = new GeoCoordinatesV2(25.5, 26.6);

        GsmSignal gsmSignal = new GsmSignal();
        gsmSignal.setCountryCode(1);
        gsmSignal.setOperatorId(2);
        gsmSignal.setCellId(100);
        gsmSignal.setLac(200);
        gsmSignal.setSignalStrength(25);

        Collection<WifiSignal> wifiSignals = Lists.newArrayList(
            new WifiSignal("TEST_MAC_1", -70),
            new WifiSignal("TEST_MAC_2", -80)
        );

        SignalsInfo signalsInfo = new SignalsInfo(wifiSignals, gsmSignal);

        String result = LbsAddRequestDataBuilder.build("test-name", clock, coordinates, signalsInfo);

        assertEquals(URLEncoder.encode("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                "<wifipool app=\"test-name\">" +
                "<chunk type=\"normal\" time=\"01011970T000000\">" + // Unix epoch start time
                "<gps lat=\"25.5\" lon=\"26.6\"/>" +
                "<bssids>" +
                "<bssid sigstr=\"-70\">TEST_MAC_1</bssid>" +
                "<bssid sigstr=\"-80\">TEST_MAC_2</bssid>" +
                "</bssids>" +
                "<cellinfos><cellinfo countrycode=\"1\" operatorid=\"2\" cellid=\"100\" lac=\"200\" sigstr=\"25\"/></cellinfos>" +
                "</chunk>" +
                "</wifipool>",
                "UTF-8"
            ),
            result
        );
    }
}
