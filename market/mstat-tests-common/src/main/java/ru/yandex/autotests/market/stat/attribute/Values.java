package ru.yandex.autotests.market.stat.attribute;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

/**
 * Created by kateleb on 25.05.15.
 */
public class Values {

    public static String empty() {
        return "";
    }

    public static String longNumber(int length) {
        return generateRandomNumber(length);
    }

    public static String maxInt() {
        return Integer.MAX_VALUE + "";
    }

    public static String string(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }

    public static String string(String prefix, int length) {
        return prefix + RandomStringUtils.randomAlphabetic(length);
    }

    public static String generateValidFee() {
        String percents = "0";
        while (percents.endsWith("0")) {
            percents = org.apache.commons.lang3.RandomStringUtils.randomNumeric(2);
        }

        return "0.0" + percents;
    }

    public static String getBoolean(String string) {
        return string.equals("1") ? "true" : "false";
    }

    public static String generateRandomNumber(int length) {
        String number = org.apache.commons.lang3.RandomStringUtils.randomNumeric(length);
        while (number.startsWith("0")) {
            number = org.apache.commons.lang3.RandomStringUtils.randomNumeric(length);
        }
        return number;
    }
}
