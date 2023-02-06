package ru.yandex.market.loyalty.core.test;

import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 26.06.17
 */
public class LoyaltySpringTestRunner extends SpringJUnit4ClassRunner {
    public LoyaltySpringTestRunner(Class<?> clazz) throws Exception {
        super(clazz);
        setPrivateFinal(ParentRunner.class.getDeclaredField("testClass"), this,
                new LoyaltyTestClass(getTestClass().getJavaClass()));

        Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations =
                new LinkedHashMap<>();

        ((LoyaltyTestClass) getTestClass()).scanInterfaceAnnotatedMethods(methodsForAnnotations);

        @SuppressWarnings("unchecked")
        Map<Class<? extends Annotation>, List<FrameworkMethod>> oldMethodsForAnnotations =
                (Map<Class<? extends Annotation>, List<FrameworkMethod>>)
                        getPrivateFinal(TestClass.class.getDeclaredField("methodsForAnnotations"), getTestClass());
        for (Map.Entry<Class<? extends Annotation>, List<FrameworkMethod>> classListEntry :
                oldMethodsForAnnotations.entrySet()) {
            methodsForAnnotations.computeIfAbsent(classListEntry.getKey(), (key) -> new ArrayList<>())
                    .addAll(classListEntry.getValue());
        }

        @SuppressWarnings("unchecked")
        Map<Class<? extends Annotation>, List<FrameworkField>> fieldsForAnnotations =
                new LinkedHashMap<>((Map<? extends Class<? extends Annotation>, ? extends List<FrameworkField>>)
                        getPrivateFinal(TestClass.class.getDeclaredField("fieldsForAnnotations"), getTestClass()));

        setPrivateFinal(TestClass.class.getDeclaredField("methodsForAnnotations"), getTestClass(),
                methodsForAnnotations);
        setPrivateFinal(TestClass.class.getDeclaredField("fieldsForAnnotations"), getTestClass(), fieldsForAnnotations);
    }

    private static void setPrivateFinal(Field field, Object object, Object newValue) throws Exception {
        Field modifiersField = Field.class.getDeclaredField("modifiers");

        try {
            field.setAccessible(true);

            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(object, newValue);
        } finally {
            field.setAccessible(false);
            modifiersField.setInt(field, field.getModifiers() & Modifier.FINAL);
            modifiersField.setAccessible(false);
        }
    }

    private static Object getPrivateFinal(Field field, Object object) throws Exception {
        Field modifiersField = Field.class.getDeclaredField("modifiers");

        try {
            field.setAccessible(true);

            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            return field.get(object);
        } finally {
            field.setAccessible(false);
            modifiersField.setInt(field, field.getModifiers() & Modifier.FINAL);
            modifiersField.setAccessible(false);
        }
    }
}
