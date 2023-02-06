package ru.yandex.market.vendors.analytics.platform;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.vendors.analytics.core.EmptyTest;


@SpringBootTest(
        properties = "spring.main.allow-bean-definition-overriding=true",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AnalyticsPlatformApp.class
)
@ContextConfiguration("/resources/functional-test-config.xml")
public abstract class FunctionalTest extends EmptyTest {

    @LocalServerPort
    protected int serverPort;

    protected String baseUrl() {
        return "http://localhost:" + serverPort;
    }

}
