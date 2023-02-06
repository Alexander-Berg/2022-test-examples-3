package ru.yandex.market.logistics.lom.service.order;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.RootAware;

public class OrderAnnotationsVersioningTest extends AbstractContextualTest {

    @DisplayName("Проверка аннотаций полей заказа для версионирования")
    @ParameterizedTest
    @MethodSource("fieldClassArgument")
    void orderFieldsCheck(Class<?> clazz) {
        softly.assertThat(RootAware.class.isAssignableFrom(clazz))
            .as("check that %s implements %s", clazz, RootAware.class)
            .isTrue();
    }

    @Nonnull
    private static Stream<Arguments> fieldClassArgument() {
        return Stream.of(Order.class.getDeclaredFields())
            .map(field -> {
                if (field.isAnnotationPresent(OneToMany.class)) {
                    return getGenericClassOfCollection(field);
                }

                if (field.isAnnotationPresent(OneToOne.class)) {
                    return field.getType();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .map(Arguments::of);
    }

    @SneakyThrows
    @Nonnull
    private static Class<?> getGenericClassOfCollection(Field field) {
        var collectionGenericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        var genericClassName = ((Class) collectionGenericType).getName();
        return Class.forName(genericClassName);
    }
}
