package ru.yandex.market.jmf.utils.date;

import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatePeriodFormattersTest {

    @Test
    public void formatTime() {
        Assertions.assertEquals("01:00-02:00",
                DatePeriodFormatters.formatTimeInterval(LocalTime.of(1, 0), LocalTime.of(2, 0)));
    }

}
