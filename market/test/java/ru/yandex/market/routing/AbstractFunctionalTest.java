package ru.yandex.market.routing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        SpringApplicationConfig.class
    }
)
@ActiveProfiles(TplProfiles.TESTS)
public abstract class AbstractFunctionalTest {
}

