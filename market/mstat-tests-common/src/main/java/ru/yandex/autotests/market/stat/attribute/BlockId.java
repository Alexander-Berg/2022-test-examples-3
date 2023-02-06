package ru.yandex.autotests.market.stat.attribute;

import java.time.LocalDateTime;

import static ru.yandex.autotests.market.stat.util.DateUtils.getMillis;

/**
 * Created by entarrion on 21.01.15.
 */
public class BlockId {

    public static String generate() {
        return generate(LocalDateTime.now());
    }

    public static String generate(LocalDateTime showTime) {
        return generateSmallFormat(showTime);
    }

    public static String generateSmallFormat(LocalDateTime showTime) {
        return Values.generateRandomNumber(10) + String.valueOf(getMillis(showTime) / 1000); //length = 20
    }
}
