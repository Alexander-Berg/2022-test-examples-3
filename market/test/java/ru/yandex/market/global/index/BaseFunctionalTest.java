package ru.yandex.market.global.index;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.global.index.config.InternalConfig;
import ru.yandex.market.global.index.config.TestsExternalConfig;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class,
                InternalConfig.class,
                TestsExternalConfig.class,
        }
)
@ActiveProfiles("functionalTest")
@TestPropertySource({"classpath:test_properties/test.properties"})
public abstract class BaseFunctionalTest {
}
