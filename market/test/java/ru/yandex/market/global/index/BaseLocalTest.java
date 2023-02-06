package ru.yandex.market.global.index;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.global.index.config.ExternalConfig;
import ru.yandex.market.global.index.config.InternalConfig;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@SuppressWarnings("SpringPropertySource")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                InternalConfig.class,
                SpringApplicationConfig.class,
                ExternalConfig.class
        }
)
@TestPropertySource({
        "file:../../../../../index/src/main/properties.d/00_application.properties",
        "file:../../../../../index/src/main/properties.d/local/00_application.properties",
        "file:../../../../../index/src/main/properties.d/local/99_local-application.properties",
})
@ActiveProfiles({"local"})
public abstract class BaseLocalTest {
}
