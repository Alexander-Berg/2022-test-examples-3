package ru.yandex.direct.dbutil.testing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import one.util.streamex.StreamEx;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

public class IgnoreAnnotationHelper {

    /**
     * Кэш чтоб каждый раз рефлекшеном не бегать по классу
     */
    private final Map<ClassToMethodPair, Boolean> ignoredMethodsCache = new ConcurrentHashMap<>();

    private final Class<?> ignoredClass;
    private final Class<? extends Annotation> annotation;

    public IgnoreAnnotationHelper(Class<? extends Annotation> annotation, Class<?> ignoredClass) {
        this.annotation = annotation;
        this.ignoredClass = ignoredClass;
    }

    /**
     * Проверяет есть ли на вызывающем методе, либо выше по стеку аннотация.
     * <p>
     * Код не очень красивый и имеет баг в случае overloaded-методов.
     * Как лучше не придумал :(
     *
     * @param lastMethodOnly Если true, смотреть только на последний метод в стеке вызовов
     */
    public boolean hasIgnoreAnnotation(boolean lastMethodOnly) {
        StreamEx<Boolean> ignoreElements = StreamEx
            .of(Thread.currentThread().getStackTrace())
            .remove(ste -> ste.getClassName().equals(ignoredClass.getCanonicalName()) ||
                    ste.getClassName().equals(IgnoreAnnotationHelper.class.getCanonicalName()))
            // Может быть лямбда с транзакцией
            .remove(ste -> ste.getClassName().equals(DslContextProvider.class.getCanonicalName()))
            .remove(ste -> ste.getMethodName().contains("lambda$"))
            .filter(ste -> ste.getClassName().startsWith("ru.yandex.direct") ||
                    ste.getClassName().startsWith("ru.yandex.autotests.direct"))
            .map(this::stackTraceElementHasAnnotation);

        if (lastMethodOnly) {
            return ignoreElements.findFirst().orElse(false);
        } else {
            return ignoreElements.anyMatch(x -> x);
        }
    }

    private boolean stackTraceElementHasAnnotation(StackTraceElement ste) {
        String className = ste.getClassName();
        String methodName = ste.getMethodName();
        return ignoredMethodsCache.computeIfAbsent(new ClassToMethodPair(className, methodName), unused -> {
            try {
                Class<?> caller = Class.forName(className);
                Method[] methods = caller.getDeclaredMethods();
                return StreamEx.of(methods)
                        .filter(m -> m.getName().equals(methodName))
                        .anyMatch(m -> m.isAnnotationPresent(annotation)
                                || m.getDeclaringClass().isAnnotationPresent(annotation));
            } catch (ClassNotFoundException e) {
                // крайне странная ситуация когда мы не нашли класс
                throw new RuntimeException(e);
            }
        });
    }

    private static class ClassToMethodPair {
        private String className;
        private String methodName;

        public ClassToMethodPair(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClassToMethodPair that = (ClassToMethodPair) o;
            return Objects.equals(className, that.className) &&
                    Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName);
        }
    }
}
