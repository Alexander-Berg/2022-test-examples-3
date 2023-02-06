package ru.yandex.market.global.checkout;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class TestTimeUtil {

    private TestTimeUtil() {
    }

    public static OffsetDateTime getTimeInDefaultTZ(LocalDateTime localDateTime) {
        return getTime(localDateTime, ApplicationDefaults.ZONE_ID);
    }


    public static OffsetDateTime getTime(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId).toOffsetDateTime();
    }
}
