package ru.yandex.market.checkout.pushapi.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class JsonUtil {

    public static Object getByPath(Object root, String path) {
        return getByPath(root,
                new ArrayList<>(Arrays.stream(path.split("/"))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList())));
    }

    public static Object getByPath(Object root, ArrayList<String> path) {
        if (path.isEmpty()) {
            return root;
        } else {
            String step = path.remove(0);
            var value = ((Map<String, Object>) root).get(step);

            if (value == null) {
                return null;
            }

            return getByPath(value, path);
        }
    }
}
