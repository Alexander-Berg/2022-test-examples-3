package ru.yandex.market.vendors.analytics.core;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author antipov93.
 */
@SpringBootTest(
        properties = "spring.main.allow-bean-definition-overriding=true",
        classes = AbstractAnalyticsApp.class
)
@ContextConfiguration("/resources/functional-test-config.xml")
public abstract class FunctionalTest extends EmptyTest {
}
