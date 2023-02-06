package ru.yandex.market.b2b.clients;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class, MockServerConfig.class
        }
)
@TestPropertySource("classpath:test_properties/99_test.properties")
@ActiveProfiles("functionalTest")
public abstract class AbstractFunctionalTest {
}

