package ru.yandex.autotests.market.stat.util;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by entarrion on 27.12.16.
 */
public class StringUtils {
    private static final Pattern PATTERN_1 = Pattern.compile("(.)([A-Z][a-z]+)");
    private static final Pattern PATTERN_2 = Pattern.compile("([a-z0-9])([A-Z])");

    public static String toUnderScore(final String input) {
        return PATTERN_2.matcher(PATTERN_1.matcher(input).replaceAll("$1_$2"))
                .replaceAll("$1_$2");
    }

    public static String toCamelCase(final String input) {
        String result = Stream.of(toUnderScore(input).split("_")).filter(it -> !it.isEmpty())
                .map(it -> Character.toUpperCase(it.charAt(0)) + it.substring(1)).collect(Collectors.joining());
        return Character.toLowerCase(result.charAt(0)) + result.substring(1);
    }
}
