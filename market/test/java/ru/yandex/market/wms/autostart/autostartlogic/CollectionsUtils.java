package ru.yandex.market.wms.autostart.autostartlogic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.common.util.collections.Pair;

public class CollectionsUtils {

    private CollectionsUtils() {
        throw new AssertionError();
    }

    public static <K, V> Map<K, V> mapOf() {
        return Collections.unmodifiableMap(new HashMap<>());
    }

    public static <K, V> Map<K, V> mapOf(Pair<K, V>... entries) {
        return new LinkedHashMap<>() {{
            for (Pair<K, V> entry : entries) {
                put(entry.first, entry.second);
            }
        }};
    }

    public static <E> List<E> listOf(E e) {
        return Collections.singletonList(e);
    }

    @SafeVarargs
    public static <E> List<E> listOf(E... e) {
        return Arrays.asList(e);
    }
}
