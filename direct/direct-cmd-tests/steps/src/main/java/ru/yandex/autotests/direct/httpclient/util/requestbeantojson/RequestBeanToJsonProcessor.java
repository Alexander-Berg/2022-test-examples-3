package ru.yandex.autotests.direct.httpclient.util.requestbeantojson;

import com.google.gson.*;

import java.util.*;

/**
 * Created by shmykov on 16.04.15.
 */
public class RequestBeanToJsonProcessor {

    public static String toJson(Object bean) {

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder
                .setFieldNamingStrategy(new RequestBeanCustomNamingStrategy())
                .setExclusionStrategies(new RequestBeanExclusionStrategy())
                .create();
        JsonElement jsonTree = gson.toJsonTree(bean);
        replaceSlashedFields(jsonTree);
        return gson.toJson(jsonTree);
    }

    /**
     * Метод проходит по списку полей и вызывает метод unchainProperty
     * для тех, которые содержат в своем имени разделитель уровней вложенности '/'.
     * Новое, развернутое поле добавляется в структуру, а старое - удаляется.
     * Если поле - объект (структура с вложенными значениями), метод вызвается для её полей рекурсивно.
     * Если поле - массив, метод вызывается для его элементов рекурсивно.
     * Если поле - простое (лист дерева) и не содержит '/' в имени - обход ветви заканчивается.
     *
     * @param element - распарсенный в JsonElement исходный бин
     */
    private static void replaceSlashedFields(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Set<String> chainedPropertiesNames = new HashSet<>();
            Set<Map.Entry<String, JsonElement>> unchainedProperties = new HashSet<>();
            for (Map.Entry<String, JsonElement> field : object.entrySet()) {
                if (field.getKey().contains("/")) {
                    List<String> chainedPropertyNames = Arrays.asList(field.getKey().split("/"));
                    JsonElement chainedPropertyValue = field.getValue();

                    Map.Entry<String, JsonElement> unchainedProperty = unchainProperty(chainedPropertyNames, chainedPropertyValue);
                    unchainedProperties.add(unchainedProperty);
                    chainedPropertiesNames.add(field.getKey());
                }
            }
            addUnchainedProperties(object, unchainedProperties);
            for (String chainedPropertyName : chainedPropertiesNames) {
                object.remove(chainedPropertyName);
            }
            for (Map.Entry<String, JsonElement> field : object.entrySet()) {
                replaceSlashedFields(field.getValue());
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement arrayElement : array) {
                replaceSlashedFields(arrayElement);
            }
        }
    }

    private static void addUnchainedProperties(JsonObject object, Set<Map.Entry<String, JsonElement>> properties) {
        for (Map.Entry<String, JsonElement> property : properties) {
            for (Map.Entry<String, JsonElement> field : object.entrySet()) {
                if (field.getKey().equals(property.getKey())) {
                    if (field.getValue().isJsonPrimitive()) {
                        object.add(property.getKey(), property.getValue());
                        return;
                    }
                    addUnchainedProperties(object.getAsJsonObject(field.getKey()), property.getValue().getAsJsonObject().entrySet());
                    return;
                }
            }
            object.add(property.getKey(), property.getValue());
        }
    }

    /**
     * Метод разворачивает поля вида "group/banner/bid":"123456" в структуру
     * "group": {
     *   "banner" : {
     *      "bid":"123456"
     *   }
     * }
     * последовательно, т.е. на первой итерации поле преобразуется к
     * "group": {
     *    "banner/bid" : "123456"
     * }
     * на следующей - к окончательному виду.
     * @param chainedPropertyNames - список элементов имени, находящихся между '/'
     * @param chainedPropertyValue - значение свойства
     * @return пара имя(верхнее имя в структуре) - значение
     */
    private static Map.Entry<String, JsonElement> unchainProperty(List<String> chainedPropertyNames, JsonElement chainedPropertyValue) {
        Iterator<String> iterator = chainedPropertyNames.iterator();
        String fieldName = iterator.next();
        if (iterator.hasNext()) {
            Map.Entry<String, JsonElement> innerField =
                    unchainProperty(chainedPropertyNames.subList(1, chainedPropertyNames.size()), chainedPropertyValue);
            JsonObject innerObject = new JsonObject();
            innerObject.add(innerField.getKey(), innerField.getValue());
            return new AbstractMap.SimpleEntry<>(fieldName, (JsonElement) innerObject);
        } else {
            return new AbstractMap.SimpleEntry<>(fieldName, chainedPropertyValue);
        }
    }
}