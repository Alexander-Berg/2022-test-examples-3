package ru.yandex.market.mbi.bot;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.mbi.bot.config.JPAConfig;
import ru.yandex.market.mbi.bot.config.WebConfig;
import ru.yandex.market.mbi.bot.liquibase.EmbeddedPostgresConfig;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                FunctionalTestConfig.class,
                JPAConfig.class,
                EmbeddedPostgresConfig.class,
                WebConfig.class,
                ServletWebServerFactoryAutoConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
@ActiveProfiles("functionalTest")
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class
})
public abstract class FunctionalTest {

}
