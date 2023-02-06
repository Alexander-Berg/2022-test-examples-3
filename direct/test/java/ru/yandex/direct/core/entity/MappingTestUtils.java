package ru.yandex.direct.core.entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

public class MappingTestUtils {

    public static Collection<Object[]> methodsAndArguments(Class<?> mappingsClass) {
        ArrayList<Object[]> parameters = new ArrayList<>();

        Method[] methods = mappingsClass.getDeclaredMethods();

        for (Method method : methods) {
            // Пропускаем методы без аргументов и лямбды
            if (method.getParameterTypes().length == 0
                    || (method.isSynthetic()
                    && method.getName().contains("lambda")
                    && method.getName().contains("$"))) {
                continue;
            }
            Class<?> argumentType = method.getParameterTypes()[0];
            if (argumentType == Boolean.class) {
                parameters.add(new Object[]{method, method.getName(), true});
                parameters.add(new Object[]{method, method.getName(), false});
            } else if (argumentType.isEnum()) {
                for (Object e : argumentType.getEnumConstants()) {
                    parameters.add(new Object[]{method, method.getName(), e});
                }
            } else {
                continue;
            }
            parameters.add(new Object[]{method, method.getName(), null});
        }
        return parameters;
    }
}
