package ru.yandex.market.yql_test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface YqlPrefilledDataTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Query {
        @Nonnull String name();
        @Nonnull String suffix();
    }

    @Nonnull Query[] queries() default {};

    String yqlMock();

}
