package ru.yandex.market.logistic.api.client.model.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.scanners.AbstractScanner;
import org.reflections.scanners.FieldAnnotationsScanner;

import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.validation.AnyRequired;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.reflections.ReflectionUtils.forName;

class AnyRequiredConsistencyTest {

    @Test
    void testConsistency() throws Exception {

        Reflections reflections = new Reflections("ru.yandex.market.logistic.api.model",
            new FieldAnnotationsScanner(), new AnnotatedParameterizedFieldTypesScanner());
        checkAnnotatedFields(reflections);
        checkAnnotatedParameterizedFields(reflections);
    }

    @Test
    void testAnnotatedFieldsNegativeScenario() {
        assertThrows(NoSuchFieldException.class, () -> {
            Reflections reflections = new Reflections(
                WrongAnnotatedDto.class,
                new FieldAnnotationsScanner(), new
                AnnotatedParameterizedFieldTypesScanner()
            );
            checkAnnotatedFields(reflections);
        });
    }

    @Test
    void testAnnotatedParameterizedFieldsNegativeScenario() {
        assertThrows(NoSuchFieldException.class, () -> {
            Reflections reflections = new Reflections(
                WrongAnnotatedDto.class,
                new FieldAnnotationsScanner(),
                new AnnotatedParameterizedFieldTypesScanner()
            );
            checkAnnotatedParameterizedFields(reflections);
        });
    }

    private void checkAnnotatedFields(Reflections reflections) throws NoSuchFieldException {
        Set<Field> fieldsAnnotatedWith = reflections.getFieldsAnnotatedWith(AnyRequired.class);

        for (Field field : fieldsAnnotatedWith) {
            AnyRequired annotation = field.getAnnotation(AnyRequired.class);
            Class<?> type = field.getType();
            checkClassHasFieldsFromAnnotation(type, annotation);
        }
    }

    private void checkAnnotatedParameterizedFields(Reflections reflections) throws NoSuchFieldException {
        Set<Field> fieldsAnnotatedParameterizedWith =
            getFieldsAnnotatedParameterizedWith(reflections, AnyRequired.class);
        for (Field field : fieldsAnnotatedParameterizedWith) {
            AnnotatedParameterizedType annotatedParameterizedType =
                (AnnotatedParameterizedType) field.getAnnotatedType();
            AnnotatedType[] annotatedActualTypeArguments =
                annotatedParameterizedType.getAnnotatedActualTypeArguments();
            for (AnnotatedType annotatedActualTypeArgument : annotatedActualTypeArguments) {
                AnyRequired annotation = annotatedActualTypeArgument.getAnnotation(AnyRequired.class);
                checkClassHasFieldsFromAnnotation((Class<?>) annotatedActualTypeArgument.getType(), annotation);
            }
        }
    }

    private void checkClassHasFieldsFromAnnotation(Class<?> cls, AnyRequired annotation) throws NoSuchFieldException {
        String[] fields = annotation.value();
        for (String field : fields) {
            cls.getDeclaredField(field);
        }
    }

    private Set<Field> getFieldsAnnotatedParameterizedWith(
        Reflections reflections, Class<? extends Annotation> annotation) {
        Store store = reflections.getStore();
        Set<Field> result = new HashSet<>();
        String simpleName = AnnotatedParameterizedFieldTypesScanner.class.getSimpleName();
        for (String annotated : store.get(simpleName, annotation.getName())) {
            String className = annotated.substring(0, annotated.lastIndexOf('.'));
            String fieldName = annotated.substring(annotated.lastIndexOf('.') + 1);
            try {
                result.add(forName(className).getDeclaredField(fieldName));
            } catch (NoSuchFieldException e) {
                throw new ReflectionsException("Can't resolve field named " + fieldName, e);
            }
        }
        return result;
    }


    static class AnnotatedParameterizedFieldTypesScanner extends AbstractScanner {
        @Override
        public void scan(Object cls, Store store) {
            final String className = getMetadataAdapter().getClassName(cls);
            try {
                snanFields(className, store);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private void snanFields(String className, Store store) throws ClassNotFoundException {
            Class<?> javaClass = Class.forName(className);
            Field[] declaredFields = javaClass.getDeclaredFields();
            for (Field field : declaredFields) {
                AnnotatedType annotatedType = field.getAnnotatedType();
                if (!(annotatedType instanceof AnnotatedParameterizedType)) {
                    continue;
                }
                AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType) annotatedType;
                AnnotatedType[] annotatedActualTypeArguments =
                    annotatedParameterizedType.getAnnotatedActualTypeArguments();
                for (AnnotatedType annotatedActualTypeArgument : annotatedActualTypeArguments) {
                    Annotation[] annotations = annotatedActualTypeArgument.getAnnotations();
                    for (Annotation annotation : annotations) {
                        String fieldAnnotation = annotation.annotationType().getName();
                        if (!acceptResult(fieldAnnotation)) {
                            continue;
                        }
                        put(store, fieldAnnotation, String.format("%s.%s", className, field.getName()));
                    }
                }
            }
        }
    }

    static class WrongAnnotatedDto {
        private @AnyRequired("") ResourceId id;

        private List<@AnyRequired("") ResourceId> ids;
    }
}
