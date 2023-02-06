package ru.yandex.market.mbo.utils;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.common.processing.OperationException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 25.12.2017
 */
public class ReflectionUtilsTest {

    private Method getDataMethod;

    @Before
    public void before() throws NoSuchMethodException {
        getDataMethod = TestService.class.getMethod("getData", long.class, Map.class);
    }

    @Test
    public void getGenericClassesReturnType() {
        Set<Class> classes = ReflectionUtils.getGenericClasses(getDataMethod.getGenericReturnType());
        assertThat(classes, containsInAnyOrder(
                Map.class,
                String.class,
                Set.class,
                Integer.class,
                Double.class
        ));
    }

    @Test
    public void getGenericClassesPrimitiveArgument() {
        Set<Class> classes = ReflectionUtils.getGenericClasses(getDataMethod.getGenericParameterTypes()[0]);
        assertThat(classes, is(Collections.singleton(long.class)));
    }

    @Test
    public void getGenericClassesComplexArgument() {
        Set<Class> classes = ReflectionUtils.getGenericClasses(getDataMethod.getGenericParameterTypes()[1]);
        assertThat(classes, containsInAnyOrder(
                Map.class,
                String.class,
                Set.class,
                Date.class,
                Character.class
        ));
    }

    @Test
    public void getExceptionClass() {
        Set<Class> classes = ReflectionUtils.getGenericClasses(getDataMethod.getGenericExceptionTypes()[0]);
        assertThat(classes, is(Collections.singleton(OperationException.class)));
    }

    @Test
    public void getAllGenericClasses() {
        Set<Class> classes = ReflectionUtils.getUsedClasses(getDataMethod);
        assertThat(classes, containsInAnyOrder(
                Map.class,     // return types
                String.class,
                Set.class,
                Double.class,
                Integer.class,
                long.class,   // + arg types
                Date.class,
                Character.class,
                OperationException.class  // + exception class
        ));
    }

    @Test
    @SuppressWarnings({"unused", "checkstyle:VisibilityModifier"})
    public void getAllFields() throws NoSuchFieldException {

        class BaseClass {
            private String basePrivate;
            public String basePublic;
        }
        class CustomClass extends BaseClass {
            private long customPrivate;
            public String customPublic;
        }

        Set<Field> fields = ReflectionUtils.getAllFields(CustomClass.class);
        assertThat("contains top public", fields, hasItem(CustomClass.class.getDeclaredField("customPublic")));
        assertThat("contains top private", fields, hasItem(CustomClass.class.getDeclaredField("customPrivate")));
        assertThat("contains inherited public", fields, hasItem(BaseClass.class.getDeclaredField("basePublic")));
        assertThat("contains inherited private", fields, hasItem(BaseClass.class.getDeclaredField("basePrivate")));
    }

    private interface TestService {
        Map<String, Map<Integer, Set<Double>>> getData(long arg0, Map<String, Map<Date, Set<Character>>> arg1)
                throws OperationException;
    }
}
