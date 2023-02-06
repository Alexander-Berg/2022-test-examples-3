package ru.yandex.autotests.market.stat.attribute;

import org.apache.commons.lang3.RandomStringUtils;
import java.time.LocalDateTime;

import static ru.yandex.autotests.market.stat.util.DateUtils.getMillis;

/**
 * Created by entarrion on 30.01.15.
 */
public class Fuid {
    private static final String HEX = "0123456789abcdef";

    public static String generateYandexFuid(LocalDateTime showTime) {
        return Long.toHexString(getMillis(showTime) / 1000) + RandomStringUtils.random(8, HEX);
    }

    public static String generateFuidForClickTime(LocalDateTime clickTime) {
        LocalDateTime fuidTime = clickTime.minusHours(3);
        return generateYandexFuid(fuidTime);
    }
}
