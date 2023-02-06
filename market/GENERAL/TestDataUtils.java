package ru.yandex.autotests.market.pushapi.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Created by jkt on 16.04.14.
 */
public class TestDataUtils {

    public static List<Object[]> asListOfArrays(Object... parameters) {
        List<Object[]> listOfArrays = new ArrayList<Object[]>();
        for (Object parameter : parameters) {
            listOfArrays.add(new Object[]{parameter});
        }
        return listOfArrays;
    }

    //for lists
    public static <T, U> List<U> convertList(List<T> from, Function<T, U> func){
        return from.stream().map(func).collect(Collectors.toList());
    }

    //for arrays
    public static <T, U> U[] convertArray(T[] from, Function<T, U> func,
                                          IntFunction<U[]> generator){
        return Arrays.stream(from).map(func).toArray(generator);
    }
}
