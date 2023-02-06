package ru.yandex.market.antifraud.orders.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.antifraud.orders.test.config.DaoTestConfiguration;

/**
 * @author dzvyagin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@ContextConfiguration(classes = {DaoTestConfiguration.class})
public @interface DaoLayerTest {
}
