package ru.yandex.market.tsum.registry.v2;

import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 25/07/2018
 */
public class RandomFieldsGenerator {
    private static final int STRING_FIELD_LENGTH = 10;

    public static String getRandomString() {
        return RandomStringUtils.random(STRING_FIELD_LENGTH, true, false);
    }

    public static int getRandomInt() {
        Random r = new Random();
        return r.nextInt();
    }

    public static int getRandomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}