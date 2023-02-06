package ru.yandex.autotests.direct.httpclient.util.jsonParser;

import com.googlecode.jxquery.BlankClass;
import com.googlecode.jxquery.creator.FieldCreator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.02.15
 */

@Retention(RUNTIME)
@Target( { FIELD })
public @interface JsonPath {
    String requestPath() default "";
    String responsePath() default "";

    Class<? extends FieldCreator<?>> creator() default BlankClass.class;

}
