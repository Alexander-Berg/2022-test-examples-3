package ru.yandex.market.tpl.core.domain.pickup.yt;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.pickup.LocalTimeInterval;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;

class YtPickupPointScheduleParserTest {

    @Test
    void parse() {
        YtPickupPointScheduleParser parser = new YtPickupPointScheduleParser(ObjectMappers.TPL_API_OBJECT_MAPPER);
        String jsonString = "[\n" +
                "    {\n" +
                "        \"1\": {\n" +
                "            \"from\": \"08:00:00\",\n" +
                "            \"to\": \"23:00:00\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"2\": {\n" +
                "            \"from\": \"08:00:00\",\n" +
                "            \"to\": \"23:00:00\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"3\": {\n" +
                "            \"from\": \"08:00:00\",\n" +
                "            \"to\": \"23:00:00\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"4\": {\n" +
                "            \"from\": \"08:00:00\",\n" +
                "            \"to\": \"23:00:00\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"5\": {\n" +
                "            \"from\": \"08:00:00\",\n" +
                "            \"to\": \"23:00:00\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"6\": {\n" +
                "            \"from\": \"08:00:00\",\n" +
                "            \"to\": \"23:00:00\"\n" +
                "        }\n" +
                "    }" +
                "]";
        Map<DayOfWeek, LocalTimeInterval> parsedSchedule = parser.parse(jsonString);
        assertThat(parsedSchedule).containsKeys(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
        );
        assertThat(parsedSchedule.get(DayOfWeek.MONDAY)).isEqualTo(new LocalTimeInterval(
                LocalTime.of(8, 0, 0),
                LocalTime.of(23, 0, 0)
        ));
    }
}
