package ru.yandex.direct.grid.processing.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import one.util.streamex.StreamEx;

import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;

import static com.google.common.base.Preconditions.checkArgument;

@ParametersAreNonnullByDefault
public class GraphQLUtils {

    public static void logErrors(List<GraphQLError> errors) {
        for (GraphQLError graphQLError : errors) {
            System.err.println(graphQLError);
            if (graphQLError instanceof ExceptionWhileDataFetching) {
                ((ExceptionWhileDataFetching) graphQLError).getException().printStackTrace(System.err);
            }
        }
    }

    public static void checkErrors(@Nullable List<GraphQLError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        String errorMessage = errors.stream()
                .map(GraphQLError::getMessage)
                .collect(Collectors.joining(";\n\t"));
        IllegalStateException illegalStateException = new IllegalStateException(errorMessage);
        errors.stream()
                .filter(err -> err instanceof ExceptionWhileDataFetching)
                .map(err -> (ExceptionWhileDataFetching) err)
                .map(ExceptionWhileDataFetching::getException)
                .forEach(illegalStateException::addSuppressed);
        throw illegalStateException;
    }

    public static List<GdValidationResult> getGdValidationResults(List<GraphQLError> errors) {
        return StreamEx.of(errors)
                .select(ExceptionWhileDataFetching.class)
                .map(ExceptionWhileDataFetching::getException)
                .select(GridValidationException.class)
                .map(GridValidationException::getValidationResult)
                .toList();
    }

    public static Map<String, Object> map(Object... args) {
        checkArgument(args.length % 2 == 0, "Args must have even number of values.");
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            map.put((String) args[i], args[i + 1]);
        }
        return map;
    }

    @SafeVarargs
    public static <T> List<T> list(T... args) {
        return new ArrayList<>(Arrays.asList(args));
    }

    @SafeVarargs
    public static <T> Set<T> set(T... args) {
        return new LinkedHashSet<>(Arrays.asList(args));
    }

    public static <T> T getDataValue(Object data, String path) {
        String[] split = path.split("/");
        Object currentData = data;
        for (String pathPart : split) {
            if (currentData instanceof Map) {
                Map map = (Map) currentData;
                currentData = map.get(pathPart);
                continue;
            }
            if (currentData instanceof List) {
                List list = (List) currentData;
                int index = Integer.parseInt(pathPart);
                currentData = list.get(index);
                continue;
            }
            throw new IllegalArgumentException(String.format("Unknown path: [%s]", path));
        }
        //noinspection unchecked
        return (T) currentData;
    }

}
