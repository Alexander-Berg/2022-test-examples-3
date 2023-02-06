package ru.yandex.calendar.logic.event;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.frontend.webNew.dto.in.ParseTest;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

    public class WebNewEventDataConverterTest {
    @Test
    public void crossMonthEvent() { // CAL-8898
        val start = MoscowTime.dateTime(2016, 10, 1, 0, 0);
        val end = MoscowTime.dateTime(2016, 11, 1, 0, 0);

        val data = ParseTest.parseEvent("{" +
                "\"startTs\": \"" + start.toString("YYYY-MM-dd'T'HH:mm:ss") + "\"," +
                "\"endTs\": \"" + end.toString("YYYY-MM-dd'T'HH:mm:ss") + "\"," +
                "\"isAllDay\": true" +
                "}");

        val event = WebNewEventDataConverter.convert(data, MoscowTime.TZ, MoscowTime.TZ);

        assertThat(event.getEvent().getStartTs()).isEqualTo(start.toInstant());
        assertThat(event.getEvent().getEndTs()).isEqualTo(end.toInstant());
    }
}
