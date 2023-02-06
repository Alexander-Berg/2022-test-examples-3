package ru.yandex.market.tpl.dora.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@ExtendWith({SpringExtension.class, CleanupEmbeddedDbExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SpringApplicationConfig.class})
@ActiveProfiles("functionalTest")
public @interface TransactionlessEmbeddedDbTest {

}
