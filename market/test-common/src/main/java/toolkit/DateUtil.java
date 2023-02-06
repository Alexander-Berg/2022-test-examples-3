package toolkit;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static final DateTimeFormatter DEFAULT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter CALENDAR_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    public static final DateTimeFormatter DATE_TIME_WITHOUT_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter CURRENT_DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyyMMdd.HHmm");

    private DateUtil() {
    }

    public static LocalDate nextBusinessDay() {
        return LocalDate.now(ZoneId.of("Europe/Moscow")).plusDays(1);
    }

    public static String currentDateTime() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DATE_TIME_WITH_ZONE);
    }

    public static String currentDate() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DEFAULT_DATE);
    }

    public static String currentDateIso() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(CURRENT_DATE_ISO);
    }

    public static String afterTomorrowDateIso() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(2).format(CURRENT_DATE_ISO);
    }

    public static String currentDateTimePlus(int seconds) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusSeconds(seconds).format(DATE_TIME_WITH_ZONE);
    }

    public static String currentDateTimePlusDays(int days) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(days).format(DATE_TIME_WITH_ZONE);
    }

    public static String currentDatePlusDays(int days) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(days).format(DEFAULT_DATE);
    }

    public static String currentDateMinusDays(int days) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).minusDays(days).format(DEFAULT_DATE);
    }

    public static String calendarDatePlusDays(int days) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(days).format(CALENDAR_DATE);
    }

    public static String tomorrowDateTime() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(1).format(DATE_TIME_WITH_ZONE);
    }

    public static String todayPlusXDateTime(int days) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(days).format(DATE_TIME_WITH_ZONE);
    }

    public static String todayPlusXDateZeroTime(int days) {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
            .plusDays(days)
            .withHour(0)
            .withMinute(0)
            .format(DATE_TIME_WITH_ZONE);
    }
}
