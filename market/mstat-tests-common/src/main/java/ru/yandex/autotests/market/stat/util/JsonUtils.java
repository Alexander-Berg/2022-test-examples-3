package ru.yandex.autotests.market.stat.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import ru.yandex.autotests.market.common.attacher.Attacher;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Created by entarrion on 01.07.16.
 */
public class JsonUtils {
    private static final JsonParser PARSER = new JsonParser();
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static JsonElement parse(String input) {
        if (input == null) return JsonUtils.parse("");
        return PARSER.parse(input);
    }

    public static boolean isFlatJson(String input) {
        try {
            List<String> lines = new ArrayList<>(Arrays.asList(input.split("\\n")));
            lines.forEach(l -> mapToJson(jsonToMap(l)));
            return true;
        } catch (JsonSyntaxException e) {
            Attacher.attachWarning(e.getMessage());
            return false;
        }
    }

    public static String format(JsonElement input) {
        return input.toString();
    }

    public static <T> String mapToJson(Map<String, T> input) {
        JsonObject result = new JsonObject();
        input.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(it -> {
                    if (Number.class.isAssignableFrom(it.getValue().getClass())) {
                        result.addProperty(it.getKey(), (Number) it.getValue());
                    } else if (it.getValue().getClass().isAssignableFrom(Boolean.class)) {
                        result.addProperty(it.getKey(), (Boolean) it.getValue());
                    } else {
                        result.addProperty(it.getKey(), (String) it.getValue());
                    }
                }
            );
        return result.toString();
    }

    public static Map<String, String> jsonToMap(String input) {
        JsonObject parse = parse(input).getAsJsonObject();
        return parse.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                it -> (it.getValue() == null || it.getValue().isJsonNull() ? "null" : it.getValue().getAsString()), (key1, key2) -> key1));
    }


    public static Map<String, JsonElement> toMapOfFirstLevel(JsonObject input) {
        return input.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, JsonPrimitive> convertFromJsonObject(JsonObject input) {
        Map<String, JsonPrimitive> kvs = new HashMap<>();
        Queue<Map.Entry<String, JsonElement>> queue = new ArrayDeque<>(input.entrySet());
        while (!queue.isEmpty()) {
            Map.Entry<String, JsonElement> entry = queue.poll();
            String prefix = entry.getKey();
            JsonElement childNode = entry.getValue();
            if (childNode.isJsonObject()) {
                queue.addAll(((JsonObject) childNode).entrySet().stream()
                    .map(it -> {
                        String nodePath = (prefix.isEmpty() ? "" : prefix + ".") + it.getKey();
                        return new AbstractMap.SimpleEntry<>(nodePath, it.getValue());
                    }).collect(Collectors.toList()));
            } else if (childNode.isJsonArray()) {
                String[] keys = entry.getKey().split("\\.");
                String key = keys[keys.length - 1];
                JsonObject o = new JsonObject();
                o.add(key, childNode);
                kvs.put(prefix, new JsonPrimitive(o.toString()));
            } else if (childNode.isJsonNull()) {
                kvs.put(prefix, new JsonPrimitive(""));
            } else if (childNode.isJsonPrimitive()) {
                kvs.put(prefix, (JsonPrimitive) childNode);
            }
        }
        return kvs;
    }

    public static JsonObject convertToJsonObjectWithStringValue(Map<String, String> input) {
        Map<String, JsonPrimitive> map = new HashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            map.put(entry.getKey(), new JsonPrimitive(entry.getValue()));
        }
        return convertToJsonObject(map);
    }

    public static JsonObject convertToJsonObject(Map<String, JsonPrimitive> input) {
        JsonObject jsonObject = new JsonObject();
        JsonParser parser = new JsonParser();
        for (Map.Entry<String, JsonPrimitive> entry : input.entrySet()) {
            JsonElement value = entry.getValue();
            if (entry.getValue().isString()) {
                try {
                    value = parser.parse(value.getAsString());
                    if (value.isJsonObject()) {
                        value = value.getAsJsonObject().entrySet().stream().map(Map.Entry::getValue)
                            .filter(JsonElement::isJsonArray).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Value must be Array"));
                    }
                } catch (JsonSyntaxException e) {
                    // Просто пытаемся распарсить как внутренний json
                }
            }
            List<String> nodes = Arrays.asList(entry.getKey().split("\\."));
            List<String> paths = nodes.subList(0, nodes.size() - 1); //Должен быть хоть один элемент
            String leaf = nodes.get(nodes.size() - 1);
            JsonObject obj = jsonObject;
            for (String node : paths) {
                if (!obj.has(node)) {
                    obj.add(node, new JsonObject());
                }
                obj = (JsonObject) obj.get(node);
            }
            obj.add(leaf, value.isJsonNull() ? new JsonPrimitive("") : value);
        }
        return jsonObject;
    }

    public static String sortJson(String json) {
        if (json == null || json.equals("")) return json;
        return GSON.toJson(GSON.fromJson(json, TreeMap.class));
    }


    public static String sortJsonExtended(String json) {
        return sortJsonExtended(parse(json));
    }

    public static String sortJsonExtended(JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }
        if (jsonElement.isJsonArray()) {
            return getElementsFromJsonArray(jsonElement.getAsJsonArray())
                .stream()
                .map(JsonUtils::sortJsonExtended)
                .collect(joining(",", "[", "]"));
        } else if (jsonElement.isJsonObject()) {
            Map<String, String> data = toMapOfFirstLevel(jsonElement.getAsJsonObject())
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> sortJsonExtended(e.getValue())));
            return mapToJson(new TreeMap<>(data));
        }
        return jsonElement.getAsString();
    }

    public static JsonObject findJsonObjectContains(JsonElement element, JsonObject mask) {
        JsonObject result;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            boolean isFind = true;
            for (Map.Entry<String, JsonElement> entry : mask.entrySet()) {
                if (!(object.has(entry.getKey()) && object.get(entry.getKey()).equals(entry.getValue()))) {
                    isFind = false;
                    break;
                }
            }
            if (isFind) {
                return object;
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                result = findJsonObjectContains(entry.getValue(), mask);
                if (result != null) {
                    return result;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement object : element.getAsJsonArray()) {
                result = findJsonObjectContains(object, mask);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static List<Integer> getNumbersFromJsonArray(JsonElement jsArray) {
        List<Integer> names = new ArrayList<>();
        if (jsArray != null) {
            JsonArray fileElem = jsArray.getAsJsonArray();
            for (int i = 0; i < fileElem.size(); i++) {
                names.add(fileElem.get(i).getAsInt());
            }
        }
        return names;
    }

    public static List<JsonElement> getElementsFromJsonArray(JsonElement jsArray) {
        List<JsonElement> names = new ArrayList<>();
        if (jsArray != null) {
            JsonArray fileElem = jsArray.getAsJsonArray();
            for (int i = 0; i < fileElem.size(); i++) {
                names.add(fileElem.get(i));
            }
        }
        return names;
    }

    public static List<String> getAllValuesForMemberName(JsonElement json, String memberName) {
        return JsonCollector.collectAllValuesForMemberName(json, memberName);
    }
}

