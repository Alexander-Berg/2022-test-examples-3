package ru.yandex.qe.mail.meetings.utils;

import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {
    private StringUtils() {
    }

    public static String trim(List<String> lines) {
        return lines.stream()
                .map(String::trim)
                .collect(Collectors.joining());
    }
}
