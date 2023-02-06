package ru.yandex.autotests.market.stat.attribute;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import ru.yandex.autotests.market.stat.date.DatePatterns;

import java.time.LocalDateTime;

/**
 * Created by entarrion on 26.01.15.
 */
public class Cookie {

    public static String generateYandexCookie(LocalDateTime showTime) {
        return RandomStringUtils.random(RandomUtils.nextInt(3) + 7, Constants.NUMBERS) + DatePatterns.UNIX_TIMESTAMP.format(showTime);
    }

    public static String generateCookieForClickTime(LocalDateTime clickTime) {
        LocalDateTime cookieGetTime = clickTime.minusHours(2);
        return generateYandexCookie(cookieGetTime);
    }

}
