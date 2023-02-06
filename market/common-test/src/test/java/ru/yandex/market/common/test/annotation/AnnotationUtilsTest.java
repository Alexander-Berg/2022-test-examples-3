package ru.yandex.market.common.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.core.annotation.AliasFor;

import static org.junit.Assert.assertEquals;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class AnnotationUtilsTest {

    @Test
    public void findAllAnnotations() {
        Set<A> result = AnnotationUtils.findAllAnnotations(D.class, A.class);
        assertEquals(5, result.size());
    }

    @Test
    public void findAllAnnotationsOrderRules() {
        List<String> values = AnnotationUtils.findAllAnnotations(Root.class, A.class).stream()
                .map(A::value)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("base", "interface", "root"), values);
    }

    @Test
    public void findAllMethodAnnotations() throws NoSuchMethodException {
        Method method = XXX.class.getDeclaredMethod("xxx");
        Set<A> methodAnnotations = AnnotationUtils.findMethodAnnotations(method, A.class);

        Set<String> values = methodAnnotations.stream().map(A::value).collect(Collectors.toSet());
        Set<String> names = methodAnnotations.stream().map(A::name).collect(Collectors.toSet());

        assertEquals(ImmutableSet.of("a", "b", "aa"), values);
        assertEquals(ImmutableSet.of("aName", "bName", "aaName"), names);
    }

    @Inherited
    @Repeatable(As.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface A {
        String value();

        String name() default "";
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface As {
        A[] value();
    }

    @Inherited()
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @A(value = "aa")
    @interface AA {

        @AliasFor(annotation = A.class, attribute = "name")
        String name() default "";
    }

    @A("asdf")
    class B {
    }

    @A("fdsa")
    @AA(name = "c")
    interface C {
    }

    @A("qwer")
    @AA(name = "d")
    class E extends B {
    }

    class D extends E implements C {
    }

    @A("root")
    class Root extends Base implements Interface {

    }

    @A("base")
    class Base {

    }

    class XXX {
        @A(value = "a",name = "aName")
        @A(value = "b",name = "bName")
        @AA(name = "aaName")
        void xxx() {
        }
    }

    @A("interface")
    interface Interface {

    }

}