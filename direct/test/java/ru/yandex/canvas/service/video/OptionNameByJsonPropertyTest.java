package ru.yandex.canvas.service.video;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.canvas.model.video.addition.Options;
import ru.yandex.canvas.model.video.addition.options.OptionName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class OptionNameByJsonPropertyTest {

    private static Map<String, OptionName> modelNameToOptionName;
    private static Map<String, Field> fieldsByJsonProperty;

    @Parameterized.Parameter
    public String jsonPropertyValue;

    @Parameterized.Parameters(name = "JsonProperty {0}")
    public static Collection<String> fieldNames() {
        List<String> fieldNames = FieldUtils.getFieldsListWithAnnotation(Options.class, JsonProperty.class).stream()
                .map(f -> f.getAnnotation(JsonProperty.class).value()).collect(Collectors.toList());
        fieldNames.addAll(simpleFields().stream().map(Field::getName).collect(Collectors.toList()));
        return fieldNames;
    }

    // поля с одно словными названия совпадающими в Java и json
    private static List<Field> simpleFields() {
        List<Field> fieldNames = new ArrayList<>();
        for (Field f : FieldUtils.getAllFieldsList(Options.class)) {
            String fieldName = f.getName();
            if (f.isAnnotationPresent(JsonProperty.class) || f.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            Method method = null;
            try {
                method = Options.class.getMethod("get" + StringUtils.capitalize(fieldName));
            } catch (NoSuchMethodException e) {
                //
            }
            if (method != null && Modifier.isPublic(method.getModifiers())) {
                fieldNames.add(f);
            }
        }
        return fieldNames;
    }

    @BeforeClass
    public static void init() {
        // По внешнему имени поля, имя поля в модели в Java
        fieldsByJsonProperty = FieldUtils.getFieldsListWithAnnotation(Options.class, JsonProperty.class).stream()
                .collect(Collectors.toMap(f -> f.getAnnotation(JsonProperty.class).value(), Function.identity()));

        for (Field f : simpleFields()) {
            fieldsByJsonProperty.putIfAbsent(f.getName(), f);
        }
        modelNameToOptionName =
                Arrays.stream(OptionName.values()).collect(Collectors.toMap(o -> o.toFieldName(),
                        Function.identity()));
    }

    @Test
    public void optionNameForJsonPropertyExists() {
        Field field = fieldsByJsonProperty.get(jsonPropertyValue);
        String fieldName = field.getName();
        assumeTrue("for JsonProperty field '" + jsonPropertyValue + "' which decorates '" + fieldName
                        + "' there is OptionName with this fieldName",
                modelNameToOptionName.containsKey(fieldName));
        assertEquals("OptionName text representation equals JsonProperty",
                modelNameToOptionName.get(fieldName).toValue(), jsonPropertyValue);
    }
}
