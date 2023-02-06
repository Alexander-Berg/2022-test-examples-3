package ru.yandex.market.logistics.lom;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.Entity;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@SuppressWarnings({"rawtypes", "unchecked"})
@DisplayName("Корректность определения сущностей")
public class EntityClassTest extends AbstractTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getClassesToValidateToString")
    @DisplayName("Проверка наличия toString у сущностей")
    public void classesToStringTest(Class<?> classToCheck) throws NoSuchMethodException {
        assertThat(classToCheck.getMethod("toString").getDeclaringClass()).isEqualTo(classToCheck);
    }

    @Nonnull
    static Stream<Arguments> getClassesToValidateToString() {
        return getAllEntityClasses().stream()
            .sorted(Comparator.comparing(Class::getName))
            .map(Arguments::of);
    }

    @Test
    @DisplayName("Все коллекции у сущностей определены с аннотацией BatchSize")
    void allEntityCollectionHasBatchSizeAnnotations() {
        Set<Class> entityClasses = getAllEntityClasses();
        softly.assertThat(entityClasses).isNotEmpty();

        for (Class entity : entityClasses) {
            Set<Field> fields = ReflectionUtils.getAllFields(entity);
            for (Field field : fields) {
                checkForBatchSizeAnnotation(entity.getSimpleName(), field);
            }
        }
    }

    private void checkForBatchSizeAnnotation(String entityClassName, Field field) {
        boolean isCollectionField = Collection.class.isAssignableFrom(field.getType());
        boolean isJsonbCollection = isCollectionField && Arrays.stream(field.getAnnotations())
            .anyMatch(
                annotation -> annotation.annotationType().equals(Type.class)
                    && ((Type) annotation).type().equals("jsonb")
            );
        if (!isCollectionField || entityClassName.contains("Builder") || isJsonbCollection) {
            return;
        }

        boolean hasBatchSizeAnnotation = Arrays.stream(field.getAnnotations())
            .anyMatch(annotation -> annotation.annotationType().equals(BatchSize.class));

        softly.assertThat(hasBatchSizeAnnotation)
            .as(
                "Field " + field.getName() + " of class " + entityClassName
                    + " is collection type, but doesn't have BatchSize annotation"
            )
            .isTrue();

    }

    @Nonnull
    private static Set<Class> getAllEntityClasses() {
        Reflections reflections = new Reflections(
            "ru.yandex.market.logistics.lom.entity",
            new SubTypesScanner(false)
        );
        return reflections.getSubTypesOf(Object.class).stream()
            .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()) && clazz.getAnnotation(Entity.class) != null)
            .collect(Collectors.toSet());
    }
}
