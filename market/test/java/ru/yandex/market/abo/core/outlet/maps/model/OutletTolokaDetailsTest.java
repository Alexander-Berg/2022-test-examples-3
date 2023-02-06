package ru.yandex.market.abo.core.outlet.maps.model;

import java.util.Arrays;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;

/**
 * @author komarovns
 * @date 28.01.19
 */
class OutletTolokaDetailsTest {
    private static final String JSON_PATH = "/toloka/outlet-toloka-details.json";
    private static final String DOMAIN = "dev.null";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void outletTolokaDetailsJsonTest() throws Exception {
        String json = IOUtils.toString(getClass().getResourceAsStream(JSON_PATH), Charsets.UTF_8);
        OutletTolokaDetails details = new OutletTolokaDetails(createOutletInfo(), createMapsOutlet(), DOMAIN, 3);
        JSONAssert.assertEquals(json, OBJECT_MAPPER.writeValueAsString(details), true);
    }

    private static MapsOutlet createMapsOutlet() {
        return MapsOutlet.builder(0, 0, false)
                .withRubrics(new LinkedHashMap<>() {{
                    put(1L, "rubric1");
                    put(2L, "rubric2");
                }}).build();
    }

    private static OutletInfo createOutletInfo() {
        Coordinates coordinates = new Coordinates(1, 2);
        GeoInfo geoInfo = new GeoInfo(coordinates, null);

        OutletInfo outletInfo = new OutletInfo(0, 0, null, "name", null, null);
        outletInfo.setGeoInfo(geoInfo);
        outletInfo.setAddress(createAddress());
        outletInfo.setPhones(Arrays.asList(createPhone("123", "456-78-90"), createPhone("495", "555-35-35")));
        outletInfo.setSchedule(new Schedule(0, Arrays.asList(
                createScheduleLine(1, 5, 10, 10),
                createScheduleLine(6, 2, 12, 6))
        ));
        return outletInfo;
    }

    private static Address createAddress() {
        return new Address.Builder()
                .setCity("Москва")
                .setStreet("Зубовский пр-д")
                .setNumber("25")
                .build();
    }

    private static PhoneNumber createPhone(String city, String number) {
        return PhoneNumber.builder()
                .setCountry("+7")
                .setCity(city)
                .setNumber(number)
                .build();
    }

    private static ScheduleLine createScheduleLine(int startDay, int days, int startHours, int hours) {
        return new ScheduleLine(ScheduleLine.DayOfWeek.values()[startDay], days - 1, startHours * 60, hours * 60);
    }
}
