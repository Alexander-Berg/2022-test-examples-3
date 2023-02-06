package ru.yandex.market.api.listener.expectations;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author dimkarp93
 */
public class ResourceHelpers {
    public static byte[] getResource(String path) {
        if (path.startsWith("/")) {
            return getResourceByAbsolutePath(path);
        }
        return getResourceByAbsolutePath(getResourceAbsolutePath(path));
    }

    private static byte[] getResourceByAbsolutePath(String resourcePath) {
        try {
            return IOUtils.toByteArray(ClassLoader.class.getResourceAsStream(resourcePath));
        } catch (Exception e) {
            throw new RuntimeException(String.format("cant load resource %s", resourcePath));
        }
    }

    private static String getResourceAbsolutePath(String relativeFileName) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace == null) {
            throw new IllegalStateException("cant get stacktrace");
        }
        for (int i = 0; i < stackTrace.length; ++i) {
            StackTraceElement stackTraceElement = stackTrace[i];
            Class aClass = getClass(stackTraceElement.getClassName());
            Optional<Method> annotationResult = tryFindMethodWithTestAnnotation(aClass, stackTraceElement.getMethodName());
            if (annotationResult.isPresent()) {
                return getResourceRelative(aClass, relativeFileName);
            }
        }
        throw new IllegalStateException(String.format("cant find location for resource = %s", relativeFileName));
    }

    private static String getResourceRelative(Class aClass, String relativeFileName) {
        String[] tokens = aClass.getCanonicalName().split("\\.");
        if (tokens.length == 0) {
            throw new IllegalArgumentException(String.format("cant extract namespace for class = %s", aClass.getCanonicalName()));
        }
        List<String> butLast = new ArrayList<>(tokens.length - 1);
        for (int i = 0; i < tokens.length - 1; ++i) {
            butLast.add(tokens[i]);
        }
        return String.format("/%s/%s",
            butLast.stream().collect(Collectors.joining("/")),
            relativeFileName);
    }

    private static Optional<Method> tryFindMethodWithTestAnnotation(Class aClass, String methodName) {
        return Arrays.stream(aClass.getMethods())
            .filter(m -> {
                if (!m.getName().equals(methodName)) {
                    return false;
                }
                return (m.getAnnotation(Test.class) != null);
            })
            .findFirst();
    }

    private static Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("cant find class by name = %s", name), e);
        }
    }
}
