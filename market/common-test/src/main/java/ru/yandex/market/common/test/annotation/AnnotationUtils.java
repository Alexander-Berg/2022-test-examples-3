package ru.yandex.market.common.test.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static <T extends Annotation> Set<T> findMethodAnnotations(Method element, Class<T> annotation) {
        if (annotation.getAnnotation(Repeatable.class) != null) {
            return AnnotatedElementUtils.getMergedRepeatableAnnotations(element, annotation);
        } else {
            T found = AnnotatedElementUtils.getMergedAnnotation(element, annotation);
            return found == null ? Collections.emptySet() : Collections.singleton(found);
        }
    }

    public static <T extends Annotation> Set<T> findAllAnnotations(Class<?> annotated, Class<T> annotation) {
        Set<T> result = new LinkedHashSet<>();
        findAllAnnotations(annotated, annotation, result);
        return result;
    }

    private static <T extends Annotation> void findAllAnnotations(
            Class<?> annotated, Class<T> annotation, Set<T> result) {
        Class parent = annotated.getSuperclass();
        if (parent != null) {
            findAllAnnotations(parent, annotation, result);
        }
        for (Class iface : annotated.getInterfaces()) {
            findAllAnnotations(iface, annotation, result);
        }

        if (annotation.getAnnotation(Repeatable.class) != null) {
            Set<T> found = AnnotatedElementUtils.getMergedRepeatableAnnotations(annotated, annotation);
            result.addAll(found);
        } else {
            T found = AnnotatedElementUtils.getMergedAnnotation(annotated, annotation);
            if (found != null) {
                result.add(found);
            }
        }
    }

}
