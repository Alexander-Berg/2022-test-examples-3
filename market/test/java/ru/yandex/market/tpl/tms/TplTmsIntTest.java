package ru.yandex.market.tpl.tms;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(
        classes = {

        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public @interface TplTmsIntTest {
}
