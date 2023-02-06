package ru.yandex.market.logistics.lom.converter.history;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

@ParametersAreNonnullByDefault
final class PathsExtractor {
    private static final int ORDER_DEPTH = 6;

    private PathsExtractor() {
        throw new UnsupportedOperationException();
    }


    /**
     * Извлечь пути до всех листовых полей класса {@code aClass} рекурсивным обходом графа,
     * исключая поля {@code exceptPaths}.
     *
     * @param aClass      класс
     * @param exceptPaths исключенные из обхода поля
     * @return список путей до листовых полей класса
     */
    @Nonnull
    static List<String> extractAllPathsExcept(Class aClass, Set<String> exceptPaths) {
        Set<String> paths = new HashSet<>();
        computeAllPaths(paths, "", aClass, exceptPaths);
        return paths.stream().sorted().collect(Collectors.toList());
    }

    private static void computeAllPaths(Set<String> paths, String currentPath, Class aClass, Set<String> exceptPaths) {
        for (Field field : aClass.getDeclaredFields()) {
            if (isStatic(field)) {
                continue;
            }

            if (isPrimitive(field)) {
                processPrimitive(paths, currentPath, field, exceptPaths);
                continue;
            }

            if (isArray(field)) {
                processArray(paths, currentPath, field, exceptPaths);
                continue;
            }

            processObject(paths, currentPath, field.getName(), field.getType(), exceptPaths);
        }
    }

    private static boolean isPrimitive(Field f) {
        return f.getType().isPrimitive()
            || f.getType().equals(Boolean.class)
            || f.getType().equals(Integer.class)
            || f.getType().equals(Long.class)
            || f.getType().equals(BigDecimal.class)
            || f.getType().equals(String.class)
            || f.getType().equals(LocalDate.class)
            || f.getType().equals(LocalTime.class)
            || f.getType().equals(LocalDateTime.class)
            || f.getType().equals(Instant.class)
            || f.getType().equals(ZonedDateTime.class)
            || f.getType().equals(OffsetDateTime.class)
            || f.getType().equals(UUID.class)
            || f.getType().isEnum();
    }

    private static boolean isStatic(Field f) {
        return Modifier.isStatic(f.getModifiers());
    }

    private static boolean isArray(Field field) {
        return Collection.class.isAssignableFrom(field.getType());
    }

    private static void processPrimitive(Set<String> paths, String currentPath, Field field, Set<String> exceptPaths) {
        String pathToField = computeFieldPath(currentPath, field.getName());
        if (!exceptPaths.contains(pathToField)) {
            checkForLoopAndAdd(paths, pathToField);
        }
    }

    @SneakyThrows
    private static void processArray(Set<String> paths, String currentPath, Field field, Set<String> exceptPaths) {
        String type = field.toGenericString().split("\\<")[1].split("\\>")[0];
        Class clazz = Class.forName(type);
        if (clazz.isEnum()) {
            processPrimitive(paths, currentPath, field, exceptPaths);
        } else {
            processObject(paths, currentPath, field.getName(), clazz, exceptPaths);
        }
    }

    private static void processObject(
        Set<String> paths,
        String currentPath,
        String name,
        Class<?> type,
        Set<String> exceptPaths
    ) {
        String pathToField = computeFieldPath(currentPath, name);
        if (!exceptPaths.contains(pathToField)) {
            checkForLoopAndAdd(paths, pathToField);
            computeAllPaths(paths, pathToField, type, exceptPaths);
        }
    }

    @Nonnull
    private static String computeFieldPath(String currentPath, String fieldName) {
        return Stream.of(currentPath, fieldName)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining("."));
    }

    private static void checkForLoopAndAdd(Set<String> paths, String pathToField) {
        if (pathToField.split("\\.").length > ORDER_DEPTH) {
            throw new IllegalStateException(
                "There might be a loop in path: " + pathToField + ". " +
                    "If so, add path to NOT_COMPARING set. Otherwise, increase ORDER_DEPTH constant"
            );
        }
        paths.add(pathToField);
    }
}
