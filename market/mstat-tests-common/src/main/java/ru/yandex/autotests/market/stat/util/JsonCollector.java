package ru.yandex.autotests.market.stat.util;

import com.google.gson.JsonElement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 18.11.16.
 */
class JsonCollector {
    public static List<String> collectAllValuesForMemberName(JsonElement json, String memberName) {
        return breadthFirstSearchNodes(json, memberName).stream().map(JsonElement::toString).collect(Collectors.toList());
    }

    public static List<JsonElement> breadthFirstSearchNodes(JsonElement json, String nodeName) {
        List<JsonElement> result = new ArrayList<>();
        Deque<JsonElement> deque = new ArrayDeque<>();
        deque.add(json);
        while (deque.size() > 0) {
            JsonElement current = deque.pollFirst();
            if (current.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : current.getAsJsonObject().entrySet()) {
                    if (entry.getKey().equals(nodeName)) {
                        result.add(entry.getValue());
                    }
                    deque.add(entry.getValue());
                }
            } else if (current.isJsonArray()) {
                for (JsonElement element : current.getAsJsonArray()) {
                    deque.add(element);
                }
            }
        }
        return result;
    }
}
