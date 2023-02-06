package ru.yandex.market.abo.core.outlet.maps.model;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 03.07.18.
 */
public class OutletTolokaResultDeserializeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String JSON = "{" +
            "  \"actualization-date\": 1529740219," +
            "  \"address\": \"Москва, Маршала Василевского ул., д. 13, корпус 1\"," +
            "  \"address-add\": null," +
            "  \"assignmentId\": \"00001ce450--5b2dfad378e8ca014151dc7e\"," +
            "  \"coordinates\": \"55.80675400189036,37.46601830656098\"," +
            "  \"coordinatesInfo\": \"yang_entry\"," +
            "  \"country\": \"RU\"," +
            "  \"emails\": []," +
            "  \"evaluation\": {" +
            "    \"accuracy\": null," +
            "    \"pool\": null" +
            "  }," +
            "  \"inn\": null," +
            "  \"language\": \"RU\"," +
            "  \"name\": \"Пункт выдачи заказов\"," +
            "  \"ogrn\": null," +
            "  \"phones\": \"+7 (915) 154-45-95\"," +
            "  \"publishing-status\": \"org\"," +
            "  \"url\": null," +
            "  \"working-time\": {" +
            "    \"intervals\": [" +
            "      {" +
            "        \"day\": \"Saturday\"," +
            "        \"time_minutes_begin\": 660," +
            "        \"time_minutes_end\": 1140" +
            "      }," +
            "      {" +
            "        \"day\": \"Weekdays\"," +
            "        \"time_minutes_begin\": 660," +
            "        \"time_minutes_end\": 1185" +
            "      }" +
            "    ]" +
            "  }," +
            "  \"working-time-string\": \"пн-пт 11:00-19:45; сб 11:00-19:00\"" +
            "}";

    @Test
    public void deserialize() throws IOException {
        OutletTolokaResult result = MAPPER.readValue(JSON, OutletTolokaResult.class);
        assertNotNull(result);

        assertEquals("Москва, Маршала Василевского ул., д. 13, корпус 1", result.getAddress());
        assertEquals(55.80675400189036, result.getLatitude().doubleValue());
        assertEquals(37.46601830656098, result.getLongitude().doubleValue());
        assertEquals(new Date(1529740219000L), result.getCheckTime());
        assertEquals("пн-пт 11:00-19:45; сб 11:00-19:00", result.getWorkTimeString());

        List<MapsScheduleLine> workIntervals = result.getWorkIntervals();
        assertEquals(2, workIntervals.size());

        MapsScheduleLine saturdayLine = workIntervals.iterator().next();
        assertEquals(MapsScheduleDay.SATURDAY, saturdayLine.getDay());
        assertEquals(660, saturdayLine.getTimeMinutesBegin());
        assertEquals(1140, saturdayLine.getTimeMinutesEnd());
    }
}