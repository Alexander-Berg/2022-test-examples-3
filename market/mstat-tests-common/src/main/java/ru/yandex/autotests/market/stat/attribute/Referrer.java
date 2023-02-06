package ru.yandex.autotests.market.stat.attribute;

/**
 * Created by kateleb on 25.08.15.
 */
public class Referrer {

    private static final String REFERER_PREFIX = "http://market.pepelac1ft.yandex.ru/search.xml?TEST_CLICK_";

    public static String generate() {
        return REFERER_PREFIX + System.nanoTime() + "_" + Values.generateRandomNumber(10);
    }

}
