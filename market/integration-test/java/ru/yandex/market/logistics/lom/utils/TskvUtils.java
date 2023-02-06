package ru.yandex.market.logistics.lom.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
@ParametersAreNonnullByDefault
public class TskvUtils {
    public static final String ENTITY_TYPES = "entity_types";
    public static final String ENTITY_VALUES = "entity_values";
    public static final String EXTRA_VALUES = "extra_values";
    public static final String EXTRA_KEYS = "extra_keys";
    public static final String CODE_KEY = "code";
    public static final String LEVEL_KEY = "level";
    public static final String PAYLOAD_KEY = "payload";
    private static final Pattern TSKV_PATTERN = Pattern.compile("([a-z_]+)=([^\\t]*)\\t?|$");

    @Nonnull
    public static Map<String, String> tskvLogToMap(String log) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = TSKV_PATTERN.matcher(log);
        while (matcher.find()) {
            result.put(matcher.group(1), matcher.group(2));
        }
        return result;
    }

    @Nonnull
    public static Map<String, String> tskvGetExtra(Map<String, String> tskvMap) {
        List<String> extraKeys = splitValues(tskvMap, EXTRA_KEYS);
        List<String> extraValues = splitValues(tskvMap, EXTRA_VALUES);
        return zip(extraKeys, extraValues);
    }

    @Nonnull
    public static Map<String, String> tskvGetEntities(Map<String, String> tskvMap) {
        List<String> entityKey = splitValues(tskvMap, ENTITY_TYPES);
        List<String> entityValues = splitValues(tskvMap, ENTITY_VALUES);
        return zip(entityKey, entityValues);
    }

    @Nonnull
    private static List<String> splitValues(Map<String, String> tskvMap, String key) {
        return Arrays.stream(tskvMap.getOrDefault(key, "").split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    }

    @Nonnull
    private static <L, R> Map<L, R> zip(List<L> first, List<R> second) {
        return IntStream.range(0, Math.min(first.size(), second.size()))
            .mapToObj(i -> Pair.of(first.get(i), second.get(i)))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
}
