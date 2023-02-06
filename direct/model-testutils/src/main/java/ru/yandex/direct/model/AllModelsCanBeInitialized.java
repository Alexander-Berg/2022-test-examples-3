package ru.yandex.direct.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.reflections.Reflections;

import static java.util.Collections.singleton;

public class AllModelsCanBeInitialized {
    public static final String PACKAGE_NAME = "ru.yandex.direct";
    public static final Set<Class<?>> BROKEN_CLASSES = new HashSet<>(singleton(TestBrokenPropertiesClass.class));

    @Test
    public void modelClassCanBeInitialized() {
        Reflections reflections = new Reflections(PACKAGE_NAME);
        var softly = new SoftAssertions();
        reflections.getSubTypesOf(Model.class)
                .forEach(modelClass ->
                        softly.assertThat(modelClass)
                                .as("class " + modelClass
                                        + " should be " + (BROKEN_CLASSES.contains(modelClass) ? "broken" : "good"))
                                .matches(x -> checkModelClass(x, BROKEN_CLASSES.contains(x)))
                );
        softly.assertAll();
    }

    private boolean checkModelClass(Class<?> clz, boolean shouldBeBroken) {
        boolean isGood;
        try {
            for (Field field : clz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    field.get(null);
                }
            }
            isGood = true;
        } catch (ExceptionInInitializerError e) {
            isGood = false;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return isGood == !shouldBeBroken;
    }
}
