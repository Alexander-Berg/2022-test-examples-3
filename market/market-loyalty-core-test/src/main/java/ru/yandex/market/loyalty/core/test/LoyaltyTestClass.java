package ru.yandex.market.loyalty.core.test;

import org.junit.internal.MethodSorter;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 26.06.17
 */
public class LoyaltyTestClass extends TestClass {
    public LoyaltyTestClass(Class<?> clazz) {
        super(clazz);
    }

    void scanInterfaceAnnotatedMethods(Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations) {
        List<Class<?>> interfaces = getSuperInterfaces(getJavaClass());
        Collections.reverse(interfaces);
        for (Class<?> eachClass : interfaces) {
            for (Method eachMethod : MethodSorter.getDeclaredMethods(eachClass)) {
                addToAnnotationLists(new FrameworkMethod(eachMethod), methodsForAnnotations);
            }
        }
    }

    private static List<Class<?>> getSuperInterfaces(Class<?> testClass) {
        ArrayList<Class<?>> results = new ArrayList<>(Arrays.asList(testClass.getInterfaces()));
        int pointer = 0;
        while (true) {
            int size = results.size();
            boolean added = false;
            for (int i = pointer; i < size; i++) {
                results.addAll(Arrays.asList(results.get(i).getInterfaces()));
                added = true;
            }
            if (!added) {
                break;
            }
            pointer = size;
        }
        return results;
    }
}
