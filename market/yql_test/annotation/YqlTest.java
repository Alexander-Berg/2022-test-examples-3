package ru.yandex.market.yql_test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface YqlTest {

    String schemasDir();

    String[] schemas();

    String csv();

    String yqlMock();

    String expectedCsv() default "";
}
