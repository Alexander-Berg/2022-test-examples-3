package ru.yandex.market.crm.operatorwindow.external;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.CallGeneralInfoResult;

public class SmartcallsCallDateTimeTest {

    private final ZoneId testZoneId = ZoneId.of("Asia/Yekaterinburg");

    @Test
    public void simple() {
        final String dateTimeStart = "2019-05-24 05:17:18";

        final ZonedDateTime parsedDate = CallGeneralInfoResult.parseDate(dateTimeStart, testZoneId);

        Assertions.assertEquals(
                ZonedDateTime.of(
                        LocalDateTime.of(2019, 5, 24, 10, 17, 18),
                        testZoneId),
                parsedDate);
    }
}
