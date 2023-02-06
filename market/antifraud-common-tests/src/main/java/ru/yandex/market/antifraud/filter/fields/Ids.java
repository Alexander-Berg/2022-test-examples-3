package ru.yandex.market.antifraud.filter.fields;

import org.apache.commons.lang3.RandomStringUtils;
import ru.yandex.market.antifraud.filter.RndUtil;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by kateleb on 20.08.15
 */
public class Ids {
    public static final String LETTERS = "abcdefghigklmnopqrstuvwxyz";
    public static final String LETTERS_HEX = "abcdef";
    public static final String NUMBERS = "0123456789";

    public static String generateWareMD5() {
        return generateWareMD5(22);
    }

    public static String generateWareMD5(int length) {
        return RandomStringUtils.random(length, LETTERS.toLowerCase() + LETTERS.toUpperCase() + "-_");
    }

    public static String generateUuid() {
        return generateUuid(32);
    }

    public static String generateUuid(int length) {
        return RandomStringUtils.random(length, NUMBERS + LETTERS_HEX.toLowerCase() + LETTERS_HEX.toUpperCase());
    }

    public static int generateHyperId() {
        return Integer.parseInt(RndUtil.randomNumeric(5));
    }

    public static String uniqueRowId() {
        return UUID.randomUUID().toString();
    }

    public static String uniqueRowIdStartingFrom(String prefix) {
        return prefix + uniqueRowId();
    }

    public static List<String> uniqueRowIdStartingFrom(String prefix, int count) {
        return Stream.generate(()->uniqueRowIdStartingFrom(prefix)).limit(count).collect(Collectors.toList());
    }

    public static int generateVclusterId() {
        return Integer.parseInt(RndUtil.randomNumeric(7));

    }
}
