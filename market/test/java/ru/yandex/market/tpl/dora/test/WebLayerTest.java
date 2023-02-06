package ru.yandex.market.tpl.dora.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@TransactionlessEmbeddedDbTest
@AutoConfigureMockMvc
@ImportAutoConfiguration({ValidationAutoConfiguration.class})
public @interface WebLayerTest {
}
