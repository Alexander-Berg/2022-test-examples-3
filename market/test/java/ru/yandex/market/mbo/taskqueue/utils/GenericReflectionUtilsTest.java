package ru.yandex.market.mbo.taskqueue.utils;

import java.math.BigInteger;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class GenericReflectionUtilsTest {

    @Test
    public void testImplements() {
        Class<?> genericClass = GenericReflectionUtils.getGenericClass(ClassStringImplementsInterface.class,
            BaseInterface.class, 0);
        Assertions.assertThat(genericClass).isEqualTo(String.class);
    }

    @Test
    public void testExtends() {
        Class<?> genericClass = GenericReflectionUtils.getGenericClass(
            ClassBigIntegerExtendsClassImplementsInterfaceBigInteger.class,
            BaseInterface.class, 0);
        Assertions.assertThat(genericClass).isEqualTo(BigInteger.class);
    }

    public interface BaseInterface<T> {

    }

    public class ClassStringImplementsInterface implements BaseInterface<String> {

    }

    public class ClassBigIntegerExtendsClassImplementsInterfaceBigInteger extends BaseClass<BigInteger> {

    }

    public abstract class BaseClass<T> implements BaseInterface<T> {

    }
}
