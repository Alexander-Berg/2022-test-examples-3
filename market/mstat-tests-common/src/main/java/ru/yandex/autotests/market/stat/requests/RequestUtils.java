package ru.yandex.autotests.market.stat.requests;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by entarrion on 22.11.16.
 */
public class RequestUtils {
    public static List<RequestParam> parseQueryParams(String query, boolean needDecode) {
        List<RequestParam> result = new ArrayList<>();
        if (Objects.nonNull(query) && !query.trim().isEmpty()) {
            Stream.of(query.trim().split("&")).map(it -> {
                String[] tmp = it.split("=", 2);
                tmp = tmp.length == 2 ? tmp : new String[]{tmp[0], ""};
                return new String[]{decode(tmp[0], needDecode), decode(tmp[1], needDecode)};
            }).forEach(it -> result.add(new RequestParam(it[0], it[1], true)));
        }
        return result;
    }

    public static List<RequestParam> parseQueryParams(String query) {
        return parseQueryParams(query, false);
    }

    public static String formatQueryParams(List<RequestParam> params) {
        return formatQueryParams(params, false);
    }

    public static String formatQueryParams(List<RequestParam> params, boolean needEncode) {
        return params.stream()
                .filter(it -> Objects.nonNull(it.getKey()))
                .map(it -> encode(it.getKey(), needEncode)
                        + "="
                        + encode((it.getValue() != null ? it.getValue() : ""), needEncode))
                .collect(Collectors.joining("&"));
    }

    public static String encode(String input, boolean needEncode) {
        return needEncode ? encode(input) : input;
    }

    public static String decode(String input, boolean needDecode) {
        return needDecode ? decode(input) : input;
    }

    public static String encode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String decode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}