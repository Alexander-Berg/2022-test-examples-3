package ru.yandex.canvas.service.video;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.canvas.model.video.addition.Options;
import ru.yandex.canvas.model.video.addition.options.OptionName;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@RunWith(Parameterized.class)
public class JsonPropertyByOptionNameTest {
    private static Map<String, Field> fieldsByJsonProperty;

    @Parameterized.Parameter
    public OptionName optionName;

    @Parameterized.Parameters(name = "OptionName {0}")
    public static Collection<OptionName> optionNameValues() {
        return Arrays.stream(OptionName.values()).filter(o -> o.toValue().contains("_"))
                .collect(Collectors.toList());
    }

    @BeforeClass
    public static void init() {
        List<Field> fieldsListWithAnnotation =
                FieldUtils.getFieldsListWithAnnotation(Options.class, JsonProperty.class);

        // По внешнему имени поля, имя поля в модели в Java
        fieldsByJsonProperty = fieldsListWithAnnotation.stream().collect(Collectors.toMap(
                f -> f.getAnnotation(JsonProperty.class).value(),
                Function.identity()
        ));
    }

    @Test
    public void jsonPropertyFieldForOptionNameExists() {
        String apiFieldName = optionName.toValue();
        assumeThat(sa-> sa.assertThat(fieldsByJsonProperty)
                .as("There is field with JsonProperty with same value as option_name")
                .containsKey(apiFieldName));
        Field modelField = fieldsByJsonProperty.get(apiFieldName);
        assertEquals("model field optionName matches OptionName=" +
                        modelField.getName() + " to FieldName=" + optionName.toFieldName(),
                modelField.getName(), optionName.toFieldName());
    }
}
