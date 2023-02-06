package ru.yandex.market.antifraud.filter;

import org.joda.time.DateTime;

public class PeriodUtils {

    /**
     * обнулить дату от милисекунд до часа
     * @param time 2014-12-01 23:34:45:345
     * @return 2014-12-01 23:00:00:000
     */
    public static DateTime truncateToHour(DateTime time) {
        return time.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    /**
     * обнулить дату от милисекунд до минуты
     * @param time 2014-12-01 23:34:45:345
     * @return 2014-12-01 23:34:00:000
     */
    public static DateTime truncateToMinute(DateTime time) {
        return time.withSecondOfMinute(0).withMillisOfSecond(0);
    }

    /**
     * обнулить дату от милисекунд до дня
     * @param time 2014-12-01 23:34:45:345
     * @return 2014-12-01 00:00:00:000
     */
    public static DateTime truncateToDay(DateTime time) {
        return time.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }


}
