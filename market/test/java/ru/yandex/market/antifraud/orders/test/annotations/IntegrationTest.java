package ru.yandex.market.antifraud.orders.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.antifraud.orders.test.config.AppTestConfiguration;

/**
 * @author dzvyagin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@WebMvcTest
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration(classes = {AppTestConfiguration.class})
public @interface IntegrationTest {
}
