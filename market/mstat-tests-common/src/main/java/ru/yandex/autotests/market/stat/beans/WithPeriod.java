package ru.yandex.autotests.market.stat.beans;

import java.time.LocalDateTime;

/**
 * Created by jkt on 07.08.14.
 */
public interface WithPeriod {

    LocalDateTime extractDayAndHour();

}
