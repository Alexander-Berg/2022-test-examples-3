package steps.utils;

import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtils {

    private static final int YEAR = 2017;
    private static final int MONTH = 3; //Month value is 0-based. e.g., 3 for April
    private static final int DAY = 22;
    private static final int HOUR = 8;
    private static final int MINUTE = 46;
    private static final int SECOND = 50;

    private DateUtils() {
    }

    public static Date getDate() {
        return getDate(YEAR, MONTH, DAY, HOUR, MINUTE, SECOND);
    }

    public static Date getDate(int year, int month, int day, int hour, int minute, int second) {
        return new GregorianCalendar(year, month, day, hour, minute, second).getTime();
    }

    public static String getDateString() {
        return getDateString(YEAR, MONTH + 1, DAY, HOUR, MINUTE, SECOND);
    }

    public static String getDateString(int year, int month, int day, int hour, int minute, int second) {
        return String.format("%s%d-%s%d-%d %s%d:%s%d:%s%d",
            getOptionalPrefix(day), day,
            getOptionalPrefix(month), month,
            year,
            getOptionalPrefix(hour), hour,
            getOptionalPrefix(minute), minute,
            getOptionalPrefix(second), second
        );
    }

    private static String getOptionalPrefix(int date) {
        String prefix = "";
        if (date < 10) {
            prefix = "0";
        }
        return prefix;
    }

}
