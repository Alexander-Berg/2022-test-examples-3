package ru.yandex.market.b2bcrm.module.pickuppoint.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@SpringJUnitConfig(classes = B2bPickupPointTestConfig.class)
@Transactional
@ActiveProfiles("test")
public @interface B2bPickupPointTests {
}
