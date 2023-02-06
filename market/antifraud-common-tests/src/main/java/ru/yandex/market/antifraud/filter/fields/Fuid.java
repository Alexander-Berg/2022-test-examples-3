package ru.yandex.market.antifraud.filter.fields;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

/**
 * Created by entarrion on 30.01.15.
 */
public class Fuid {
    private static final String HEX = "0123456789abcdef";

    public static String generateYandexFuid(DateTime showTime) {
        return Long.toHexString(showTime.getMillis() / 1000) + RandomStringUtils.random(8, HEX);
    }

    public static String generateFuidForClickTime(DateTime clickTime) {
        DateTime fuidTime = clickTime.minusHours(3);
        return generateYandexFuid(fuidTime);
    }
}
