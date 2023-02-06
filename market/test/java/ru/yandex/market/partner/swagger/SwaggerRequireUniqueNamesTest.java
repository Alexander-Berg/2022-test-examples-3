package ru.yandex.market.partner.swagger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.annotations.ApiModel;
import it.unimi.dsi.fastutil.Pair;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

public class SwaggerRequireUniqueNamesTest {

    /**
     * Тест проверяет отсутствие дубликатов имен в моделях сваггера. Это нужно для контроля обратной совместимости
     * сваггер моделей. Новая модель дублкат может перетерть старую модель и вместо этого релиз мастер будет
     * сравнивать старую модель с новой, а не изменение одной модели.
     * <br><br>
     * <b>Что делать если обнаружены дубликаты?</b> Нужно найти модели из текстового вывода теста и исправить
     * io.swagger.annotations.ApiModel#value() таким образом чтобы дубликатов не было.
     */
    @Test
    void testNoDuplicateNames() {
        Reflections reflections = new Reflections("ru.yandex.market.partner");
        Set<Class<?>> apiModelClasses = reflections.getTypesAnnotatedWith(ApiModel.class);
        Map<String, Class<?>> apiModelNameToClass = new HashMap<>();
        Map<String, Set<Class<?>>> duplicates = new HashMap<>();


        for (Class<?> clazz : apiModelClasses) {
            String annotationValue = clazz.getAnnotation(ApiModel.class).value();
            String apiModelName = StringUtils.isEmpty(annotationValue) ? clazz.getSimpleName() : annotationValue;

            Class<?> existingClass = apiModelNameToClass.get(apiModelName);

            if (existingClass == null) {
                apiModelNameToClass.put(apiModelName, clazz);
                continue;
            }

            Set<Class<?>> delcaredClasses = new HashSet<>(List.of(existingClass.getDeclaredClasses()));
            delcaredClasses.addAll(Arrays.asList(clazz.getDeclaredClasses()));

            //не считать дубликатом одинаковые модели в классе и inner классе. Случай с билдерами.
            if (delcaredClasses.contains(clazz) || delcaredClasses.contains(existingClass)) {
                continue;
            }
            apiModelNameToClass.put(apiModelName, clazz);

            duplicates.computeIfAbsent(apiModelName, s -> new HashSet<>()).add(clazz);
            duplicates.get(apiModelName).add(existingClass);
        }
        Assertions.assertTrue(duplicates.isEmpty(), "Duplicates: " + duplicates.toString());
    }


    /**
     * Проверяет, что swagger definitions только на латыни.
     *  <br><br>
     * <b>Что делать если обнаружены модели на лытни?</b> Нужно найти модели из текстового вывода теста и
     * описание поместить в {@link ApiModel#description()}, а в {@link ApiModel#value()} установить имя класса.
     */
    @Test
    void testOnlyLatinNames() {
        Reflections reflections = new Reflections("ru.yandex.market.partner");
        Set<Class<?>> apiModelClasses = reflections.getTypesAnnotatedWith(ApiModel.class);

        List<Pair<String, String>> nonLatinClasses = apiModelClasses.stream()
                .filter(c -> !c.getAnnotation(ApiModel.class).value().isEmpty())
                .filter(c -> !StringUtils.isAlphanumeric(c.getAnnotation(ApiModel.class).value()))
                .map(c -> Pair.of(c.getName(), c.getAnnotation(ApiModel.class).value()))
                .collect(Collectors.toList());
        Assertions.assertTrue(nonLatinClasses.isEmpty(), "Api models with non latin names: " + nonLatinClasses);
    }
}
