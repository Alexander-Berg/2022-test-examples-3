package ru.yandex.market.antifraud.filter.fields;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.RndUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by entarrion on 26.01.15.
 */
public class Cookie {

    public static String generateYandexCookie(DateTime showTime) {
        return RndUtil.randomNumeric(RndUtil.nextInt(3) + 7) + (showTime.getMillis() / 1000);
    }

    public static String generateCookieForClickTime(DateTime clickTime) {
        DateTime cookieGetTime = clickTime.minusHours(2);
        return generateYandexCookie(cookieGetTime);
    }

    public static Map<String, String> cookieToMap(String cookies) {
        if (cookies == null) return new HashMap<>();
        return Stream.of(cookies.split(";")).map(it -> {
            String[] res = it.trim().split("=", 2);
            return res.length == 2 ? res : new String[]{res[0], ""};
        }).collect(Collectors.toMap(it -> it[0], it -> it[1], (key1, key2) -> key1));
    }

    public static String mapToCookie(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(it -> !(it.getKey() == null || it.getKey().trim().isEmpty() || it.getValue() == null || it.getValue().trim().isEmpty()))
                .map(it -> it.getKey().trim() + "=" + it.getValue().trim()).collect(Collectors.joining("; "));
    }

    public static String updateCookie(String cookies, Map<String, String> params) {
        Map<String, String> result = cookieToMap(cookies);
        for (Map.Entry<String, String> param : params.entrySet()) {
            result = updateCookie(result, param.getKey(), param.getValue());
        }
        return mapToCookie(result);
    }

    public static String updateCookie(String cookies, String key, String value) {
        return mapToCookie(updateCookie(cookieToMap(cookies), key, value));
    }

    public static String getParamFromCookie(String cookies, String key) {
        return cookieToMap(cookies).getOrDefault(key, "");
    }

    private static Map<String, String> updateCookie(Map<String, String> cookies, String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return cookies;
        }

        if (value == null) {
            cookies.remove(key);
            return cookies;
        }
        cookies.put(key, value);
        return cookies;
    }
}
