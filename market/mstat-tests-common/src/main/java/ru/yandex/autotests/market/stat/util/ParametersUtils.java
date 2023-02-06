package ru.yandex.autotests.market.stat.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jkt on 28.04.14.
 */
public class ParametersUtils {

    public static List<Object[]> asParameters(Object... values) {
        return asParameters(Arrays.asList(values));
    }

    public static List<Object[]> asParameters(Collection<?> values) {
        return values.stream().map(it -> new Object[]{it}).collect(Collectors.toList());
    }
}
