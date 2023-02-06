package ru.yandex.market.core.param.model;

import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wadim
 */
class ParamTypeTest {

    private static final Map<ValueType, Class> CLASS_MAP = ImmutableMap.<ValueType, Class>builder()
            .put(ValueType.STRING, String.class)
            .put(ValueType.NUMBER, Number.class)
            .put(ValueType.BOOLEAN, Boolean.class)
            .put(ValueType.DATE, Date.class)
            .build();


    @Test
    void testGetParamType() {
        assertThat(ParamType.RESERVED_1).isSameAs(ParamType.getParamType(1));
        assertThat(ParamType.NULL).isSameAs(ParamType.getParamType(100000));
    }

    @Test
    void testDefaultValues() {
        // Контракт теста необходимо поддерживать
        assertThat(ValueType.values())
                .as("You must support a new type in this test")
                .hasSize(5);

        for (ParamType type : ParamType.values()) {
            checkDefaultValue(type);
        }
    }


    private static void checkDefaultValue(ParamType type) {
        ValueType valueType = type.getValueType();
        Object defaultValue = type.getDefaultValue();

        if (valueType == ValueType.UNDEFINED || defaultValue == null) {
            // Не можем проверить, так как нет данных о типе/значении
            return;
        }

        // Проверить что ValueType нам известен
        Class<?> valueTypeClass = CLASS_MAP.get(valueType);
        assertThat(valueTypeClass)
                .as("Unknown value type %s for parameter %s", valueType, type)
                .isNotNull();

        // Проверить что значение "по умолчанию" имеет корректный тип
        boolean correctType = valueTypeClass.isInstance(defaultValue);
        assertThat(correctType)
                .as("Incorrect type of default value %s for parameter %s", defaultValue, type)
                .isTrue();
    }

}
