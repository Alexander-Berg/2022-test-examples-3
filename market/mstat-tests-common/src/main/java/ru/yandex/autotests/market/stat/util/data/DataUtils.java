package ru.yandex.autotests.market.stat.util.data;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by jkt on 07.08.14.
 */
public class DataUtils {

    public static Map<String, String> getFieldBeanioNames(Class dataType) {
        List<Field> fields = Arrays.asList(dataType.getDeclaredFields());
        return fields.stream().filter(DataUtils::hasFieldAnnotation)
            .filter(f -> !f.getName().equals(f.getAnnotation(org.beanio.annotation.Field.class).name())).collect(toMap(
                Field::getName,
                field -> field.getAnnotation(org.beanio.annotation.Field.class).name()
            ));
    }

    private static boolean hasFieldAnnotation(Field field) {
        org.beanio.annotation.Field f = field.getAnnotation(org.beanio.annotation.Field.class);
        return f != null && StringUtils.isNotEmpty(f.name());
    }

    public static String getRidOfUnderscore(String field) {
        List<String> nameParts = new ArrayList<>(Arrays.asList(field.split("_"))).stream().filter(part -> part.length() > 0).collect(toList());
        String head = nameParts.remove(0);
        return nameParts.stream().map(it -> it.substring(0, 1).toUpperCase() + it.substring(1)).collect(joining("", head, ""));
    }

}
