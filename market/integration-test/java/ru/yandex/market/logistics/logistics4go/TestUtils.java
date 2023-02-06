package ru.yandex.market.logistics.logistics4go;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Lombok;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class TestUtils {
    @Nonnull
    public static <T> Arguments notNullValidation(
        T source,
        String path
    ) {
        return getValidationViolationArguments(
            "Отсутствует " + path,
            source,
            path,
            null,
            "must not be null"
        );
    }

    @Nonnull
    public static <T> Arguments getValidationViolationArguments(
        String displayName,
        T source,
        String path,
        @Nullable Object value,
        String message
    ) {
        TestUtils.setter(path, value).accept(source);
        return Arguments.of(
            displayName,
            source,
            new ValidationViolation().field(path).message(message)
        );
    }

    @Nonnull
    public static <T> Consumer<T> setter(String path, @Nullable Object value) {
        return (T source) -> {
            try {
                String[] fieldNames = path.split("\\.");
                Object object = source;
                Class<?> clazz = object.getClass();
                Field field = FieldUtils.getField(clazz, fieldNames[0], true);
                for (int i = 1; i < fieldNames.length; i++) {
                    object = field.get(object);
                    clazz = object.getClass();
                    field = FieldUtils.getField(clazz, fieldNames[i], true);
                }

                field.set(object, value);
            } catch (Throwable t) {
                throw Lombok.sneakyThrow(t);
            }
        };
    }
}
