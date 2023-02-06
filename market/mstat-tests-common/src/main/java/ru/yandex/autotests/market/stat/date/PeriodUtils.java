package ru.yandex.autotests.market.stat.date;

import java.time.LocalDateTime;

/**
 * Created by jkt on 07.08.14.
 */
public class PeriodUtils {

    /**
     * обнулить дату от милисекунд до часа
     * @param time 2014-12-01 23:34:45:345
     * @return 2014-12-01 23:00:00:000
     */
    public static LocalDateTime truncateToHour(LocalDateTime time) {
        return time.withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * обнулить дату от милисекунд до минуты
     * @param time 2014-12-01 23:34:45:345
     * @return 2014-12-01 23:34:00:000
     */
    public static LocalDateTime truncateToMinute(LocalDateTime time) {
        return time.withSecond(0).withNano(0);
    }

    /**
     * обнулить дату от милисекунд до дня
     * @param time 2014-12-01 23:34:45:345
     * @return 2014-12-01 00:00:00:000
     */
    public static LocalDateTime truncateToDay(LocalDateTime time) {
        return time.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }


}
