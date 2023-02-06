package ru.yandex.autotests.market.stat.date;

import java.time.LocalDateTime;

/**
 * Created by entarrion on 18.05.15.
 */
interface TimeHandler {
    LocalDateTime parse(String source);

    String format(LocalDateTime date);
}
