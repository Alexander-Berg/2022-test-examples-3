package ru.yandex.market.ff.model.entity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Defaults;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * ATTENTION: этот утиль нужен исключительно для контроля того,
 * что новые поля сущностей корректно обрабатываются copy конструктором.
 * <p>
 * Использовать только в случаее крайней необходимости.
 */
final class ReflectionTestUtils {

    private ReflectionTestUtils() {
        throw new UnsupportedOperationException("Can't create instance of utility method.");
    }

    /**
     * @return имена полей, для которых значение равно <p>>null</p> или дефолтному (в случае примитивов).
     */
    public static <T> List<String> findFieldNamesWithNullOrDefaultValue(T object, Class<T> cls) {
        List<Field> fields = List.of(FieldUtils.getAllFields(cls));
        var fieldNamesWithNullValue = fields.stream()
                .filter(field -> isValueNullOrDefault(object, field))
                .map(Field::getName)
                .collect(Collectors.toList());

        return fieldNamesWithNullValue;
    }

    /**
     * Попарно извлекает значения полей {@param actual} и {@param expected},
     * которые передает в {@param fieldValuesConsumer}.
     *
     * @param actual         полученное значение.
     * @param expected       ожидаемое значение.
     * @param valuesConsumer компаратор значений полей {@param actual} и {@param expected}.
     */
    public static <T> void compareFieldValues(T actual, T expected, Class<T> cls,
                                              AssertingFieldValuesConsumer valuesConsumer) {

        for (Field field : FieldUtils.getAllFields(cls)) {
            Object actualValue = readFieldValue(actual, field.getName());
            Object expectedValue = readFieldValue(expected, field.getName());
            valuesConsumer.accept(field.getName(), actualValue, expectedValue);
        }
    }

    private static boolean isValueNullOrDefault(Object object, Field field) {
        var value = readFieldValue(object, field.getName());
        var defaultValue = Defaults.defaultValue(field.getType());

        return value == defaultValue;
    }

    private static Object readFieldValue(Object object, String fieldName) {
        try {
            return FieldUtils.readField(object, fieldName, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public interface AssertingFieldValuesConsumer {
        void accept(String fieldName, Object actualFieldValue, Object expectedFieldValue);
    }

}
