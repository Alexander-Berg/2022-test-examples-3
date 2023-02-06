package ru.yandex.chemodan.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.reflection.FieldX;

/**
 * @author akirakozov
 */
public class ReflectionUtils {

    public static void setFieldNewValue(String fieldName, Object target, Object newFieldValue) {
        try {
            Field field = target.getClass().getField(fieldName);
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(target, newFieldValue);
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }

    public static Object getField(Object target, String fieldName) {
        FieldX field = ClassX.wrap(target.getClass()).getAllDeclaredInstanceFields().find(f -> f.getName().equals(fieldName)).getOrThrow("field not found");
        field.setAccessible(true);

        return field.get(target);
    }

}
