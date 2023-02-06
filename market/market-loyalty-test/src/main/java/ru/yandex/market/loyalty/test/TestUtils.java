package ru.yandex.market.loyalty.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestUtils {
    private TestUtils() {
    }

    /**
     * Проверяет, что поля класса имеют корректные ацессоры => могут быть корректно (де)сериализованы
     *
     * @param clazz class for check
     * @return wrong fields list
     */
    public static List<String> checkClassPropertyNamesIsRight(Class<?> clazz) {
        var fields = Stream.of(Arrays.stream(clazz.getDeclaredFields()), Arrays.stream(clazz.getFields()))
                .flatMap(s -> s)
                .distinct()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toMap(field -> field.getName().toLowerCase(), Field::getType, (a, b) -> a));

        var methods = Stream.of(Arrays.stream(clazz.getDeclaredMethods()), Arrays.stream(clazz.getMethods()))
                .flatMap(s -> s)
                .distinct()
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .collect(Collectors.toMap(method -> method.getName().toLowerCase(), method -> method, (a, b) -> a));

        var constructorParams = Arrays.stream(clazz.getDeclaredConstructors())
                .flatMap(cons -> Arrays.stream(cons.getParameters()))
                .collect(Collectors.toMap(param -> param.getName().toLowerCase(), param -> param, (a, b) -> a));

        var result = new ArrayList<String>();
        for (var field : fields.entrySet()) {
            var hasGetter = methods.containsKey("get" + field.getKey()) || methods.containsKey("is" + field.getKey())
                    || methods.values().stream()
                    .anyMatch(m -> m.getReturnType().equals(field.getValue())
                            && m.isAnnotationPresent(JsonProperty.class)
                            && m.getAnnotation(JsonProperty.class).value().equalsIgnoreCase(field.getKey()));

            var hasSetter = methods.containsKey("set" + field.getValue())
                    || constructorParams.containsKey(field.getKey())
                    || methods.values().stream()
                    .anyMatch(m -> m.getReturnType().equals(Void.class)
                            && m.getParameterTypes()[0].equals(field.getValue())
                            && m.isAnnotationPresent(JsonProperty.class)
                            && m.getAnnotation(JsonProperty.class).value().equalsIgnoreCase(field.getKey()))
                    || constructorParams.values().stream()
                    .anyMatch(p -> p.getType().equals(field.getValue())
                            && p.isAnnotationPresent(JsonProperty.class)
                            && p.getAnnotation(JsonProperty.class).value().equalsIgnoreCase(field.getKey()));

            if (!hasGetter && hasSetter) {
                result.add(clazz.getName() + "." + field.getKey());
            }
        }

        return result;
    }
}
