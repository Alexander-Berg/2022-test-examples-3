package ru.yandex.market.wms.packing.utils;

import java.util.Collection;
import java.util.stream.Collectors;

@SuppressWarnings("HideUtilityClassConstructor")
public class TestCollectionUtils {

    public static <T> T head(Collection<T> collection) {
        return collection.stream().findFirst().get();
    }

    public static <T> Collection<T> tail(Collection<T> collection) {
        return collection.stream().skip(1).collect(Collectors.toList());
    }
}
