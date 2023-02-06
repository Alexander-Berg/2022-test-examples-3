package ru.yandex.market.pers.qa;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.pers.qa.config.CoreConfig;
import ru.yandex.market.pers.qa.config.InternalConfig;
import ru.yandex.market.pers.qa.mock.QaMockConfiguration;

/**
 * @author korolyov
 * 20.06.18
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
        QaMockConfiguration.class,
        CoreConfig.class,
        InternalConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource("classpath:/test-application.properties")
@EnableWebMvc
public class PersQaApiTest extends PersQACoreTest {

    @Override
    protected void resetMocks() {
        PersQaServiceMockFactory.resetMocks();
    }
}
